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
import org.bson.{BSON, BasicBSONObject}
import org.ooploogr.Ooploogr
import reactivemongo.utils.Converters

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
    val t1 = new org.bson.types.BSONTimestamp(1361152018, 0)
    val doc = new BasicBSONObject("ts", t1)
    val buffer = ChannelBuffers.dynamicBuffer(ByteOrder.LITTLE_ENDIAN, 256)
    buffer.writeBytes(BSON.encode(doc))
    Assert.assertEquals(BSONTimestamp(5846103402194403328L), Ooploogr.toBSONTimestamp(1361152018))
  }

}
