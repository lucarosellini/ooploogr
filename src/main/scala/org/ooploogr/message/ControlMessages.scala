package org.ooploogr.message

import reactivemongo.bson.{BSONTimestamp, BSONDocument}

case object StartProcessing
case object Ack
case object StopProcessing
case class ProcessDocument(doc:BSONDocument)
case class SaveTimestamp(ts: BSONTimestamp)
