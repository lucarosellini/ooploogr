package org.ooploogr.actors

import java.nio.ByteOrder
import java.text.DecimalFormat
import java.util.Date

import akka.pattern.ask
import akka.actor.{Props, Actor}
import akka.util.Timeout
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.jboss.netty.buffer.ChannelBuffers
import org.ooploogr.message._
import play.api.libs.iteratee.Iteratee
import reactivemongo.api.{QueryOpts, MongoDriver}
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.BSONDocument
import reactivemongo.bson.buffer.DefaultBufferHandler.BSONTimestampBufferHandler
import reactivemongo.core.netty.{ChannelBufferWritableBuffer, ChannelBufferReadableBuffer}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import reactivemongo.bson._

import org.bson.{BasicBSONObject, BSON}
import scala.util.{Try, Failure, Success}
import reactivemongo.bson.DefaultBSONHandlers._

/**
 * Tailing actor companion object
 */
object OplogTailActor {

  /**
   * Converts an integer to a BSONTimestamp object
   * @param t
   * @return
   */
  def toBSONTimestamp(t: Int, inc: Int = 0): BSONTimestamp = {
    val buffer =
    ChannelBuffers.dynamicBuffer(ByteOrder.LITTLE_ENDIAN, 256)

    buffer.writeBytes(BSON.encode(new BasicBSONObject("ts", new org.bson.types.BSONTimestamp(t, inc))))


    val bf = ChannelBufferReadableBuffer(buffer)

    val bsonValue =  BSONDocument.read(bf).get("ts")

    bsonValue.get.asInstanceOf[BSONTimestamp]
  }

  /**
   * Returns an integer from a BSONTimestamp object.
   *
   * @param t
   * @return
   */
  def fromBSONTimestamp(t: BSONTimestamp): Int = {
    val reverseDoc = BSONDocument("ts" -> t.asInstanceOf[BSONValue])

    val buffer = ChannelBufferWritableBuffer()
    BSONDocument.write(reverseDoc, buffer)

    val backTobson = BSON.decode(buffer.buffer.array())
    backTobson.get("ts").asInstanceOf[org.bson.types.BSONTimestamp].getTime
  }

  /**
   * Flattens the provided BSONDocument and converts it to string
   * @param doc
   * @return
   */
  def printFlat(doc: BSONDocument): String = {
    BSONDocument.pretty(doc)

    //"{ "+printFlat(doc.stream.iterator)+" }"
  }

  private def printFlat(it: Iterator[Try[(String,BSONValue)]]): String = {
    (for(v <- it) yield {
      val e = v.get
      e._2 match {
        case elem : BSONTimestamp => e._1 + ": " + fromBSONTimestamp(elem)
        case doc : BSONDocument => e._1 + ": { " + printFlat(doc.stream.iterator) + " }"
        case array :BSONArray => e._1 + ": [ " + printFlat(array.iterator) +" ]"
        case _ => e._1 + ": " + e._2.toString
      }
    }).mkString(", ")
  }
}

/**
 * Main tailing actor. Opens a connection to the ReplicaSet and starts tailing the oplog.
 *
 *
 * @param sourceHost
 * @param lastTimestamp
 * @param includedCollections
 * @param excludedCollections
 */
class OplogTailActor(sourceHost: String, lastTimestamp: String,
                     includedCollections: List[String] = Nil,
                     excludedCollections: List[String] = Nil) extends Actor with LazyLogging {
  import scala.concurrent.ExecutionContext.Implicits.global


  var OPERATIONS_READ: Int = 0
  var OPERATIONS_SKIPPED: Int = 0
  var INSERT_COUNT: Int = 0
  var UPDATE_COUNT: Int = 0
  var DELETE_COUNT: Int = 0
  val REPORT_INTERVAL = 100L
  val LONG_FORMAT = new DecimalFormat("###,###")

  implicit val timeout = Timeout(1 minutes)
  val TIMEOUT = Duration(30000, MILLISECONDS)


  var lastOutput = System.currentTimeMillis()
  val START_TIME = System.currentTimeMillis()
  val processorActor = context.system.actorOf(Props[KafkaProducerActor], "kafkaProducerActor")
  val lastTimestampActor = context.system.actorOf(Props[LastTimestampActor], "lastTimestampActor")

  logger.debug("Connecting to MongoDB")

  val driver = new MongoDriver(context.system)

  val connection = driver.connection(List(sourceHost))
  val db = connection.db("local")
  val collection = db.collection[BSONCollection]("oplog.rs")

  logger.info(s"Included collections: $includedCollections")

  /**
   * Tailing logic, when a new message arrives from the oplog, the message is sent to a processor actor.
   */
  def startOplogTail(): Unit ={

    var query = BSONDocument()
    val timestamp = parseTimestamp(lastTimestamp)

    logger.info(s"lastTimestamp: $timestamp")

    if (timestamp > 0) {
      query = BSONDocument("ts" -> BSONDocument("$gt" -> OplogTailActor.toBSONTimestamp(timestamp).asInstanceOf[BSONValue]))

      logger.info(s"query: ${OplogTailActor.printFlat(query)}")
    }

    val cursor = collection.find(query)
      .options(QueryOpts().tailable.awaitData)
      .cursor[BSONDocument]

    cursor.enumerate().apply(Iteratee.foreach {
      doc =>
        self ! ProcessDocument(doc)
    })
  }

  override def receive = {
    case StartProcessing => {
      logger.info("Received StartProcessing")
      startOplogTail()
    }
    case StopProcessing => {
      logger.info("Received StopProcessing")
      var future = processorActor ? StopProcessing
      Await.ready(future, TIMEOUT)
      future = lastTimestampActor ? StopProcessing
      Await.ready(future, TIMEOUT)
      closeConnections()
      sender ! Ack
      context stop self
    }
    case ProcessDocument(doc:BSONDocument) => {
      processDoc(doc)
    }
    case _ => logger.warn("Unknown message")
  }

  private def parseTimestamp(fromTime: String): Int = {
    val future = lastTimestampActor ? GetTimestamp
    val res = Await.result(future, TIMEOUT)

    res match {
      case ts:Some[BSONTimestamp] => OplogTailActor.fromBSONTimestamp(ts.get)
      case _ => 0
    }

    /*
    var ret = 0
    if (null != fromTime) {
      try {
        ret = java.lang.Integer.parseInt(fromTime)
      }
      catch {
        case e: Exception =>
          logger.error("Timestamp provided is not a valid number: " + fromTime,e)
      }
    }
    ret
    */
  }

  /**
   * Performs the actual processing
   * @param doc
   */
  def processDoc(doc: BSONDocument) {
    if (shouldProcess(doc, includedCollections, excludedCollections)) {
      val msg = ProcessDocument(doc)

      val future = processorActor ? msg
      future onSuccess {
        case Ack => {
          val operationType: String = doc.get("op").get.asInstanceOf[BSONString].value
          if ("i".equals(operationType)) {
            INSERT_COUNT = INSERT_COUNT + 1
          }
          else if ("d".equals(operationType)) {
            DELETE_COUNT = DELETE_COUNT + 1
          }else if ("u".equals(operationType)) {
            UPDATE_COUNT = UPDATE_COUNT + 1
          }
          else if ("c".equals(operationType)) {
            logger.info("Operation type c")
          }

          lastTimestampActor ? SaveTimestamp(doc.get("ts").get.asInstanceOf[BSONTimestamp])
        }

      }

      future onFailure {
        case failure: Throwable => logger.error(s"failed to process doc:\n ${OplogTailActor.printFlat(doc)}", failure)
        case _ => logger.error("Cannot determine failure object")
      }
      Await.ready(future, TIMEOUT)

      //processRecord(doc)
      OplogTailActor.synchronized {
        OPERATIONS_READ = OPERATIONS_READ + 1
      }
    }
    else
      OplogTailActor.synchronized {
        OPERATIONS_SKIPPED = OPERATIONS_SKIPPED + 1
      }

    OplogTailActor.synchronized {
      val durationSinceLastOutput = System.currentTimeMillis() - lastOutput;
      if (durationSinceLastOutput > REPORT_INTERVAL) {
        report(INSERT_COUNT,
          UPDATE_COUNT,
          DELETE_COUNT,
          OPERATIONS_READ,
          OPERATIONS_SKIPPED,
          System.currentTimeMillis() - START_TIME,
          doc.get("ts").get.asInstanceOf[BSONTimestamp])

          //OplogTailActor.fromBSONTimestamp(doc.get("ts").get.asInstanceOf[BSONTimestamp]));
        lastOutput = System.currentTimeMillis();
      }
    }
  }

  /**
   * Prints progress.
   *
   * @param inserts
   * @param updates
   * @param deletes
   * @param totalCount
   * @param skips
   * @param duration
   * @param timestamp
   */
  private def report(inserts: Long, updates: Long, deletes: Long, totalCount: Long, skips: Long, duration: Long, timestamp: BSONTimestamp) {
    val brate = totalCount.asInstanceOf[Double] / (duration / 1000.0)

    val ts = OplogTailActor.fromBSONTimestamp(timestamp)
    logger.info("inserts: "
      + LONG_FORMAT.format(inserts) + ", updates: " + LONG_FORMAT.format(updates)
      + ", deletes: " + LONG_FORMAT.format(deletes) + ", skips: " + LONG_FORMAT.format(skips)
      + " (" + LONG_FORMAT.format(brate) + " req/sec), last ts: " + new Date(ts * 1000L));
  }

  /**
   * Determines if the provided BSONDocument should be processed or not.
   *
   * @param doc
   * @param includedCollections
   * @param excludedCollections
   * @return
   */
  private def shouldProcess(doc: BSONDocument, includedCollections: List[String], excludedCollections: List[String]): Boolean = {

    val namespace = doc.get("ns").get.asInstanceOf[BSONString].value
    if (null == namespace || "".equals(namespace))
      return false
    if (excludedCollections.size == 0 && includedCollections.size == 0)
      return true
    if (excludedCollections.contains(namespace))
      return false
    if (includedCollections.contains(namespace) || includedCollections.contains("*"))
      return true
    if (namespace.indexOf('.') > 0 && includedCollections.contains(namespace.substring(0, namespace.indexOf('.'))))
      return true

    false
  }

  /**
   * Closes mongo connection
   * @return
   */
  private def closeConnections(): Int = {
    logger.info("Closing connections")
    val sourceClose = connection.askClose()(TIMEOUT)
    waitForClose(sourceClose, "source")
    0
  }

  private def waitForClose(closeFuture: Future[_], name: String) = {
    closeFuture.onComplete {
      case Failure(e) =>
        logger.error("Could not close " + name + " connection: ",e)
      case Success(lasterror) => {
        logger.info("Closed " + name + " connection")
      }
    }
    Await.ready(closeFuture, TIMEOUT)
  }
}