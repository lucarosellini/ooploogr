package org.ooploogr.message

import reactivemongo.bson.BSONDocument

case object StartProcessing
case object Ack
case object StopProcessing
case class ProcessDocument(doc:BSONDocument)