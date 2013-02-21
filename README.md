Ooploogr
========

This is a simple tool for playing back MongoDB oplog entries from one server to another. 

It uses the [ReactiveMongo](http://reactivemongo.org/) scala driver which is currently the only driver that I am aware of (besides the native C one) that does not seem affected by the [repeated fields issue](https://jira.mongodb.org/browse/SERVER-1606)

Ironically, without this ability to play back oplogs, ReactiveMongo would normally not have been the best fit for this job because of its' asynchronous design. Oplogs need to be played back in order! So care had to be taken to wait for each command to finish before moving on to the next one. So definitely doing a lot of blocking here which quite the opposite of the driver's intent.

Install
-------
- `git clone git://github.com/gpstathis/ooploogr.git`
- `cd ooploogr`
- `sbt assembly`
- Run `java -jar target/ooploogr-assembly-1.0.jar` for usage

Usage
-----

Shamelessly copied from the excellent [wordnik-oss tools](https://github.com/wordnik/wordnik-oss):

<pre>
usage: Ooploogr
 -s : source database host[:port]
 -d : destination database host[:port]
 [-t : oplog timestamp from which to start playback]
 [-c : CSV of collections to process, scoped to the db (database.collection), ! will exclude]
 [-r : collection re-targeting (format: {SOURCE}={TARGET})]
 [-R : database re-targeting (format: {SOURCE}={TARGET})]
</pre>
