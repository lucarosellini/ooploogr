package org.ooploogr.actors

import akka.actor.Actor
import org.ooploogr.message.DocMessage
import reactivemongo.bson.BSONDocument

/**
 * Processes a BSONDocument.
 *
 * // TODO
 */
class DocProcessorActor extends Actor{

  override def receive = {
    case DocMessage(doc: BSONDocument) => println("received: "
      +OplogTailActor.printFlat(doc))
    case _ => println("Received unknown message")
  }
}