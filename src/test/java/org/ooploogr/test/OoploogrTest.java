/**
 * OoploogrTest.java - Traackr, Inc.
 * 
 * This document set is the property of Traackr, Inc., a Massachusetts
 * Corporation, and contains confidential and trade secret information. It
 * cannot be transferred from the custody or control of Traackr except as
 * authorized in writing by an officer of Traackr. Neither this item nor the
 * information it contains can be used, transferred, reproduced, published,
 * or disclosed, in whole or in part, directly or indirectly, except as
 * expressly authorized by an officer of Traackr, pursuant to written
 * agreement.
 * 
 * Copyright 2012-2013 Traackr, Inc. All Rights Reserved.
 */
package org.ooploogr.test;

import org.bson.types.BSONTimestamp;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.*;

/**
 * @author gstathis
 *         Created on: 2/15/13
 */
public class OoploogrTest {
  
  private static Logger log       = LoggerFactory.getLogger(OoploogrTest.class);
  
  String                _host     = "ec2-107-22-152-94.compute-1.amazonaws.com";
  Integer               _port     = 27017;
  String                _db       = "local";
  String                _username = "mongo";
  String                _password = "MongoproD4TraackR";
  Integer               _time     = 1360904400;
  
  @Test
  @Ignore
  public void playingWithMongo() throws Exception {
    MongoClient mongoClient = new MongoClient(_host, _port);
    DB db = mongoClient.getDB(_db);
    boolean auth = db.authenticate(_username, _password.toCharArray());
    log.debug("auth: {}", auth);
    DBCollection oplog = db.getCollection("oplog.rs");
    BasicDBObject query = new BasicDBObject("ts", new BasicDBObject("$gt", new BSONTimestamp(_time, 0)));
    DBCursor cursor = oplog.find(query);
    cursor.addOption(Bytes.QUERYOPTION_OPLOGREPLAY);
    cursor.addOption(Bytes.QUERYOPTION_TAILABLE);
    cursor.addOption(Bytes.QUERYOPTION_AWAITDATA);
    
    while (cursor.hasNext()) {
      DBObject x = cursor.next();
      if (x.get("op").equals("u"))
        log.debug("{}", x);
    }
  }
  
}
