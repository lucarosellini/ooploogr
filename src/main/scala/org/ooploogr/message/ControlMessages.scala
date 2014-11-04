package org.ooploogr.message

import reactivemongo.bson.BSONDocument

case class StartProcessing()
case class StopProcessing()
case class ProcessDocument(doc:BSONDocument)