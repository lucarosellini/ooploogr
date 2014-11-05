package org.ooploogr

import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory
import reactivemongo.bson._
import reactivemongo.bson.DefaultBSONHandlers._
import org.jboss.netty.buffer.ChannelBuffers
import java.nio.ByteOrder
import org.bson.{BasicBSONObject, BSON}
import java.lang.String
import java.util.{Date, StringTokenizer}
import reactivemongo.api._
import play.api.libs.iteratee.Iteratee
import concurrent.{Await, Future}
import reactivemongo.bson.BSONTimestamp
import util.Failure
import reactivemongo.core.commands.RawCommand
import util.Success
import reactivemongo.bson.BSONString
import scala.concurrent.duration._
import java.text.DecimalFormat
import akka.actor.ActorSystem
import org.ooploogr.actors.KafkaProducerActor
import akka.actor.Props
import org.ooploogr.message.{StartProcessing, StopProcessing, DocMessage}
import org.ooploogr.actors.OplogTailActor
import org.ooploogr.actors.OplogTailActor
import scala.collection.immutable.List

/**
 * Application entry point.
 * Starts the Akka system and starts the tailing actor.
 *
 * 
 * @author Luca Rosellini
 */
object Main extends App {
  var SOURCE_HOST: String = null
  var FROM_TIME: String = null
  var COLLECTION_STRING: String = null
  val LONG_FORMAT = new DecimalFormat("###,###")

  Console.println("Starting Ooploogr")
  
  val START_TIME = System.currentTimeMillis()
  if (!parseArgs(args)) {
    usage()
    sys.exit()
  }

  var collections: (List[String], List[String]) = parseCollections(COLLECTION_STRING)

  val akkaSystem = ActorSystem("AkkaActorSystem")

  val prop = Props(classOf[OplogTailActor], SOURCE_HOST, FROM_TIME, collections._1, collections._2)
  
  val tailActor = akkaSystem.actorOf(prop, "oplogTailActor")

  tailActor ! StartProcessing

  sys addShutdownHook {
    Console.println("Shutdown hook caught.")
    tailActor ! StopProcessing
    Console.println("Done shutting down.")
  }

  /**
   * Parses the included/excluded collection names.
   * This information will we used by the tailing actor to determine
   * if a specific change we got from the oplog should be processed or not.
   *
   * @param collectionString
   * @return
   */
  private def parseCollections(collectionString: String): (List[String], List[String]) = {
    var collectionsToAdd: List[String] = List()
    var collectionsToSkip: List[String] = List()
    if (collectionString != null) {
      var hasIncludes = false
      val collectionNames: Array[String] = collectionString.split(",")
      collectionNames.foreach {
        collectionName =>
          if (collectionName.startsWith("!")) {
            //	skip it
            collectionsToSkip = collectionsToSkip.::(collectionName.substring(1).trim())
          }
          else {
            collectionsToAdd = collectionsToAdd.::(collectionName.trim())
            hasIncludes = true
          }
      }
      if (!hasIncludes) {
        collectionsToAdd.::("*")
      }
    }
    else {
      collectionsToAdd.::("*")
    }
    (collectionsToAdd, collectionsToSkip)
  }

  /**
   * Parses command line arguments.
   *
   * @param args
   * @return
   */
  private def parseArgs(args: Array[String]): Boolean = {
    var skip: Boolean = false
    for (i <- 0 to args.length - 1) {
      if (!skip) {
        args(i) match {
          case "-s" => SOURCE_HOST = args(i + 1); skip = true
          case "-t" => FROM_TIME = args(i + 1); skip = true
          case "-c" => COLLECTION_STRING = args(i + 1); skip = true
          case _ => Console.println("Unknown parameter " + args(i))
          return false
        }
      }
      else
        skip = false
    }
    if (null == SOURCE_HOST)
      SOURCE_HOST = "localhost"
    true
  }

  private def usage() {
    Console.println("usage: Ooploogr")
    Console.println(" -s : source database host[:port]")
    Console.println(" [-t : oplog timestamp from which to start playback]")
    Console.println(" [-c : CSV of collections to process, scoped to the db (database.collection), ! will exclude]")
  }
}
