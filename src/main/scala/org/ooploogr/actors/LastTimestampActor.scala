package org.ooploogr.actors

import akka.actor.Actor
import com.datastax.driver.core.{ConsistencyLevel, Cluster}
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.querybuilder.QueryBuilder.{eq => dstxeq}
import com.datastax.driver.core.utils.Bytes
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.ooploogr.message.{SaveTimestamp, Ack, StopProcessing}
import reactivemongo.bson.BSONTimestamp

object GetTimestamp


/**
 * Created by luca on 7/11/14.
 */
class LastTimestampActor extends Actor with LazyLogging with ConfigAware  {

  val cluster = Cluster.builder().addContactPoint(cassandraHost).withPort(cassandraPort).build()
  val metadata = cluster.getMetadata
  val session = cluster.connect()
  logger.debug(s"Connected to cluster: ${metadata.getClusterName}")

  session.execute(s"CREATE KEYSPACE IF NOT EXISTS ${cassandraKeyspace} WITH REPLICATION = {'class': '${cassandraKeyspaceReplicationStrategy}', 'replication_factor': ${cassandraKeyspaceRF} }")
  session.execute(s"CREATE TABLE IF NOT EXISTS ${cassandraKeyspace}.${lastTimestampTable} (word TEXT PRIMARY KEY, ts INT)")
  session.execute(s"USE ${cassandraKeyspace}")

  override def receive = {
    case GetTimestamp =>
      val stmt = QueryBuilder.select("ts").from(cassandraKeyspace, lastTimestampTable).where(dstxeq("word", lastTimestampKey)).setConsistencyLevel(ConsistencyLevel.QUORUM)
      val rs = session.execute(stmt)

      val res = if (!rs.isExhausted) Some(OplogTailActor.toBSONTimestamp(rs.one().getInt("ts"))) else None

      sender ! res

    case SaveTimestamp(ts: BSONTimestamp) =>
      val timestamp = OplogTailActor.fromBSONTimestamp(ts)

      val stmt = QueryBuilder.insertInto(cassandraKeyspace, lastTimestampTable).value("word", lastTimestampKey).value("ts", timestamp).setConsistencyLevel(ConsistencyLevel.QUORUM)
      session.execute(stmt)

    case StopProcessing => {
      logger.info("Received StopProcessing")
      cluster.close()
      sender ! Ack
      context stop self

    }
  }
}
