package org.ooploogr.test.scala

import org.junit.{Ignore, Assert, Test}
import org.ooploogr.actors.OplogTailActor
import java.lang.{Exception}
import java.lang.String
import play.api.libs.iteratee.Iteratee
import reactivemongo.api._
import reactivemongo.bson._
import reactivemongo.bson.DefaultBSONHandlers._
import org.jboss.netty.buffer.ChannelBuffers
import java.nio.ByteOrder
import org.bson.{BSONObject, BSON, BasicBSONObject}
import org.ooploogr.Main
import java.util.Date

/**
 * @author gstathis
 *         Created on: 2/15/13
 */
class OoploogrTestScala {

  @Test
  def playingWithBSONDocument(): Unit = {
    val doc = BSONDocument()
      .add("$set" -> BSONDocument().add("foo" -> BSONBoolean(true)))
      .add("$set" -> BSONDocument().add("bar" -> BSONBoolean(true)))
    Assert.assertEquals(2, doc.elements.length)
  }

  @Test
  def playingWithBSONTimestamp(): Unit = {
    Assert.assertEquals(BSONTimestamp(5846103402194403328L), OplogTailActor.toBSONTimestamp(1361152018))
    Assert.assertEquals(1361152018, OplogTailActor.fromBSONTimestamp(BSONTimestamp(5846103402194403328L)))
  }

  /*
  @Test
  def playingWithBSONDocumentPrint(): Unit = {
    val doc = BSONDocument()
      .add("$set" -> BSONDocument().add("foo" -> BSONBoolean(true)))
      .add("$set" -> BSONDocument().add("bar" -> BSONBoolean(true)))
    Assert.assertEquals("{ $set: { foo: BSONBoolean(true) }, $set: { bar: BSONBoolean(true) } }", OplogTailActor.printFlat(doc))
  }*/

}
