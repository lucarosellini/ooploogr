package org.ooploogr.actors

import java.nio.ByteOrder
import java.util.Properties

import akka.actor.Actor
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.slf4j.LazyLogging
import kafka.producer.{KeyedMessage, Producer, ProducerConfig, Partitioner}
import org.jboss.netty.buffer.ChannelBuffers
import org.ooploogr.message.{Ack, DocMessage}
import reactivemongo.bson._
import reactivemongo.core.netty.ChannelBufferWritableBuffer
import scala.collection.JavaConversions._

/**
 * Processes a BSONDocument.
 *
 * // TODO
 */
class KafkaProducerActor extends Actor with LazyLogging{

  val config = ConfigFactory.load()
  val topic = "mongo.oplog"
  val brokers = config.getStringList("kafka.brokers").mkString(",")

  val props = new Properties();

  props.put("metadata.broker.list", brokers);
  props.put("serializer.class", "kafka.serializer.StringEncoder");
  //props.put("partitioner.class", "example.producer.SimplePartitioner");
  props.put("request.required.acks", "1");

  val producerConfig = new ProducerConfig(props);
  val producer = new Producer[String, String](producerConfig)

  logger.debug(""+brokers)

  override def receive = {
    case DocMessage(doc: BSONDocument) => {

      val operationType: String = doc.get("op").get.asInstanceOf[BSONString].value
      val id = ""+doc.get("h").get.asInstanceOf[BSONLong].value
      //logger.debug(s"Received oplog message with id: $id")

      //logger.debug(s"received: ${OplogTailActor.printFlat(doc).substring(0,60)}")

      val msg = new KeyedMessage(topic, id, BSONDocument.pretty(doc))

      producer.send(msg)

      sender ! Ack
    }
    case _ => println("Received unknown message")
  }

  class OplogPartitioner extends Partitioner {
    override def partition(key: Any, numPartitions: Int): Int = {
      0
    }
  }
}