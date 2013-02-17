name := "ooploogr"

version := "1.0"

scalaVersion := "2.10.0"

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "Traackr repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "Local Maven" at Path.userHome.asFile.toURI.toURL + ".m2/repository"

libraryDependencies += "junit"                  % "junit"                   % "4.11"    % "test"

libraryDependencies += "org.slf4j"              % "slf4j-api"               % "1.7.2"   % "test"

libraryDependencies += "ch.qos.logback"         % "logback-classic"         % "1.0.9"   % "test"

libraryDependencies += "com.novocode"           % "junit-interface"         % "0.9"     % "test->default"

libraryDependencies += "org.mongodb"            % "mongo-java-driver"     % "2.10.1"

libraryDependencies += "com.allanbank.mongodb"  % "mongodb-async-driver"  % "1.1.0"

libraryDependencies += "org.reactivemongo"      %% "reactivemongo"        % "0.8"