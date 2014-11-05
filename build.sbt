import AssemblyKeys._

assemblySettings

name := "ooploogr"

version := "1.0"

scalaVersion := "2.10.4"

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "Maven Central" at "http://repo1.maven.org/maven2/"

resolvers += "Local Maven" at Path.userHome.asFile.toURI.toURL + ".m2/repository"

libraryDependencies += "junit"                  % "junit"                   % "4.11"    % "test"

libraryDependencies += "com.novocode"           % "junit-interface"         % "0.9"     % "test->default"

libraryDependencies += "org.mongodb"            % "mongo-java-driver"       % "2.10.1"

libraryDependencies += "org.reactivemongo"      %% "reactivemongo"          % "0.10.5.0.akka23"

libraryDependencies += "com.typesafe.akka" 		  %% "akka-actor" 		% "2.3.6"

libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.2"

libraryDependencies ++= Seq("org.apache.kafka" % "kafka_2.10" % "0.8.1.1"
                                  exclude("com.sun.jdmk","jmxtools")
                                  exclude("com.sun.jmx","jmxri")
)