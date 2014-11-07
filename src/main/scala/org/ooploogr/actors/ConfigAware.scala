package org.ooploogr.actors

import com.typesafe.config.ConfigFactory
import scala.collection.JavaConversions._

/**
 * Created by luca on 7/11/14.
 */
trait ConfigAware {
  val config = ConfigFactory.load()

  val kafkaBrokers = config.getStringList("kafka.brokers").mkString(",")
  val cassandraHost = config.getString("cassandra_host")
  val cassandraPort = config.getInt("cassandra_port")
  val cassandraKeyspace = config.getString("cassandra_keyspace")
  val cassandraKeyspaceRF = config.getInt("cassandra_keyspace_rf")
  val cassandraKeyspaceReplicationStrategy = config.getString("cassandra_keyspace_replication_strategy")
  val lastTimestampTable = config.getString("cassandra_table_lastTimestamp")
  val lastTimestampKey = config.getString("cassandra_table_lastTimestamp_key")
}
