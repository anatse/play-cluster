## Project  play cluster

Akka based example for implements final state machine
Apache Cassandra used as storage for akka Persistent FSM

Build commands:
1. Downloads and install SBT (https://www.scala-sbt.org/)
1. In project root directory run sbt
1. Inside sbt shell run command flow / dist
1. run command dist

Also you may use docker:publichLocal sbt command

### Getting started

#### Install Apache Cassandra
You may use docker to run Cassandra. There are two ways to start application:
1. Change network port for docker properly: by default journal's host:port mapped to 127.0.0.1:9042
1. Change configuration application.conf, adding cassandra-journal.contact-points = ["host:port"], with regard to desired connection parameters

#### Start
1. Locate file ./flow/target/universal/flow-1.0-SNAPSHOT.zip
1. Extract it and start main cluster nodes
   1. Start script ./bin/flow 2551
   1. Start script ./bin/flow 2552
1. Extract file ./play/target/universal/play-cluster-1.0-SNAPSHOT.zip
1. Start ./bin/play-cluster
1. Open http://localhost:9000

Use

For testing REST service use http://localhost:9000/c?flowName=testFlow&state=readProducts

