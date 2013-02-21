package org.ooploogr.test.scala

import org.junit.{Ignore, Assert, Test}
import org.slf4j.LoggerFactory
import java.lang.{Exception}
import java.lang.String
import play.api.libs.iteratee.Iteratee
import reactivemongo.api._
import reactivemongo.bson._
import reactivemongo.bson.handlers.DefaultBSONHandlers._
import org.jboss.netty.buffer.ChannelBuffers
import java.nio.ByteOrder
import org.bson.{BSONObject, BSON, BasicBSONObject}
import org.ooploogr.Ooploogr
import reactivemongo.utils.Converters
import java.util.Date

/**
 * @author gstathis
 *         Created on: 2/15/13
 */
class OoploogrTestScala {

  @Test
  def playingWithBSONDocument(): Unit = {
    val doc = BSONDocument()
      .append("$set" -> BSONDocument().append("foo" -> BSONBoolean(true)))
      .append("$set" -> BSONDocument().append("bar" -> BSONBoolean(true)))
    Assert.assertEquals(2, doc.elements.length)
  }

  @Test
  def playingWithBSONTimestamp(): Unit = {
    Assert.assertEquals(BSONTimestamp(5846103402194403328L), Ooploogr.toBSONTimestamp(1361152018))
    Assert.assertEquals(1361152018, Ooploogr.fromBSONTimestamp(BSONTimestamp(5846103402194403328L)))
  }

  @Test
  def playingWithBSONDocumentPrint(): Unit = {
    val doc = BSONDocument()
      .append("$set" -> BSONDocument().append("foo" -> BSONBoolean(true)))
      .append("$set" -> BSONDocument().append("bar" -> BSONBoolean(true)))
    Assert.assertEquals("{ $set: { foo: BSONBoolean(true) }, $set: { bar: BSONBoolean(true) } }", Ooploogr.printFlat(doc))
  }

}
