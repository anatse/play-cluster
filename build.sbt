import com.typesafe.sbt.SbtMultiJvm.multiJvmSettings
import com.typesafe.sbt.SbtMultiJvm.MultiJvmKeys.MultiJvm
import sbt.Keys.resolvers

val akkaVersion = "2.5.13"
val scalaV = "2.12.6"
val jacksonVersion = "2.9.5"

val depOverrides = Seq(
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-management" % akkaVersion,
  "com.typesafe.akka" %% "play-streams" % "2.6.15",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.0",
  "com.google.guava"% "guava" % "22.0",
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "org.eclipse.sisu"% "org.eclipse.sisu.plexus" % "0.3.2"
)

// build for packaging sbt docker:publishLocal
lazy val play = (project in file("play")).enablePlugins(PlayScala, PlayAkkaHttp2Support, JavaAppPackaging)
  .settings(multiJvmSettings: _*)
  .settings (
    name := """play-cluster""",
    organization := "org.wtf",
    version := "1.0-SNAPSHOT",
    scalaVersion := scalaV,
    fork in run := true,
    // add this to cmd file for JVM >= 9
    //javaOptions in run += "--add-modules java.xml.bind",
    dependencyOverrides ++= depOverrides,
    libraryDependencies ++= Seq(
      guice,
      filters,

      "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-metrics" % akkaVersion,

      // Akka cluster management (https://developer.lightbend.com/docs/akka-management/current/akka-management.html)
      "com.lightbend.akka.management" %% "akka-management" % "0.10.0",

      // Sigar library
      "io.kamon" % "sigar-loader" % "1+",

      "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test,
      "com.typesafe.akka" %% "akka-multi-node-testkit" % akkaVersion % Test,
      "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test
    )
  )
  .configs (MultiJvm)
  .dependsOn(shared)

lazy val flow = (project in file("flow"))
  .settings(multiJvmSettings: _*)
  .settings(
    name := "flow",
    version := "1.0-SNAPSHOT",
    scalaVersion := scalaV,
    fork in run := true,
    mainClass := Some("org.wtf.flow.FlowApp"),
    dependencyOverrides ++= depOverrides,
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-metrics" % akkaVersion,

      // Persistent & DData
      "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
      "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8",
      "com.typesafe.akka" %% "akka-persistence-query" % akkaVersion,

      // Cassandra
      "com.typesafe.akka" %% "akka-persistence-cassandra" % "0.85",
      "com.typesafe.akka" %% "akka-persistence-cassandra-launcher" % "0.85" % Test,

      // JSON
      "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion,
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion,

      // Sigar library
      "io.kamon" % "sigar-loader" % "1+",

      // Log
      "ch.qos.logback" % "logback-classic" % "1.2.3" % Runtime,

      "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test,
      "com.typesafe.akka" %% "akka-multi-node-testkit" % akkaVersion % Test,
      "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test
    )
  )
  .enablePlugins(JavaAppPackaging)
  .configs (MultiJvm)
  .dependsOn(shared)

lazy val shared = (project in file("shared")).
  settings(scalaVersion := scalaV)

onLoad in Global ~= (_ andThen ("project play" :: _))

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "org.wtf.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "org.wtf.binders._"
