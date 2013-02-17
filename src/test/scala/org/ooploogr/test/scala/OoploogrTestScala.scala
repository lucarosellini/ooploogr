package org.ooploogr.test.scala

import org.junit.Test
import org.slf4j.LoggerFactory
import java.lang.{String, Exception}
import java.lang.String
import reactivemongo.bson.{BSONTimestamp, BSONDocument}
import play.api.libs.iteratee.Iteratee
import reactivemongo.api._
import reactivemongo.bson._
import reactivemongo.bson.handlers.DefaultBSONHandlers._
import concurrent.Future

/**
 * @author gstathis
 *         Created on: 2/15/13
 */
class OoploogrTestScala {

  val log = LoggerFactory.getLogger(getClass)
  //var _host: String = "localhost"
  var _host: String = "ec2-107-22-152-94.compute-1.amazonaws.com"
  var _port: Integer = 27017
  var _db: String = "local"
  var _username: String = "mongo"
  var _password: String = "MongoproD4TraackR"
  var _time: Integer = 1360904400

  @Test
  @throws(classOf[Exception])
  def playingWithReactiveMongo(): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global

    val connection = MongoConnection(List(_host + ":" + _port))
    //val db = connection(_db)
    val db = connection("traackr")
    //val collection = db("oplog.rs")
    val collection = db("monitors")

    val query = BSONDocument("ts" -> BSONDocument("$gt" -> BSONTimestamp(1359435600)))
    //val query = BSONDocument()
    val cursor = collection.find(query)

    cursor.enumerate.apply(Iteratee.foreach { doc =>
      //println("found document: " + BSONDocument.pretty(doc))
      log.debug("{}", BSONDocument.pretty(doc))
    })

    //connection.close()
  }
}
