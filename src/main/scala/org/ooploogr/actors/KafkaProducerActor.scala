package org.ooploogr.actors

import java.nio.ByteOrder
import java.util.Properties

import akka.actor.Actor
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.slf4j.LazyLogging
import kafka.kryo.bson.BSONDocumentKryoEncoder
import kafka.producer.{KeyedMessage, Producer, ProducerConfig, Partitioner}
import org.jboss.netty.buffer.ChannelBuffers
import org.ooploogr.message.{ProcessDocument, StopProcessing, Ack}
import reactivemongo.bson._
import reactivemongo.core.netty.ChannelBufferWritableBuffer
import scala.collection.JavaConversions._
//import uk.gov.hmrc.mongo.ExtraBSONHandlers._

/**
 * Processes a BSONDocument.
 */
class KafkaProducerActor extends Actor with LazyLogging with ConfigAware {

  val topic = "mongo.oplog"

  val props = new Properties()

  props.put("metadata.broker.list", kafkaBrokers);
  props.put("serializer.class", "kafka.kryo.bson.BSONDocumentKryoEncoder");
  props.put("key.serializer.class", "kafka.serializer.StringEncoder");
  //props.put("partitioner.class", "example.producer.SimplePartitioner");
  props.put("request.required.acks", "1");

  val producerConfig = new ProducerConfig(props);
  val producer = new Producer[String, BSONDocument](producerConfig)

  logger.debug(""+kafkaBrokers)

  override def receive = {
    case ProcessDocument(doc: BSONDocument) => {

      val operationType: String = doc.get("op").get.asInstanceOf[BSONString].value
      val id = ""+doc.get("h").get.asInstanceOf[BSONLong].value
      //logger.debug(s"Received oplog message with id: $id")

      //logger.debug(s"received: ${OplogTailActor.printFlat(doc).substring(0,60)}")

      val msg = new KeyedMessage(topic, id, doc)

      producer.send(msg)

      sender ! Ack
    }
    case StopProcessing => {
      logger.info("Received StopProcessing")
      producer.close()

      sender ! Ack
      context stop self
    }
    case _ => println("Received unknown message")
  }

  class OplogPartitioner extends Partitioner {
    override def partition(key: Any, numPartitions: Int): Int = {
      0
    }
  }
}