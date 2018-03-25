import com.typesafe.sbt.SbtMultiJvm.multiJvmSettings
import com.typesafe.sbt.SbtMultiJvm.MultiJvmKeys.MultiJvm
import sbt.Keys.resolvers

val akkaVersion = "2.5.11"
val scalaV = "2.12.5"

// build for packaging sbt docker:publishLocal
lazy val play = (project in file("play")).enablePlugins(PlayScala, PlayAkkaHttp2Support, JavaServerAppPackaging)
  .settings(multiJvmSettings: _*)
  .settings (
    name := """play-cluster""",
    organization := "org.wtf",
    version := "1.0-SNAPSHOT",
    scalaVersion := scalaV,
    fork in run := true,
    // add this to cmd file for JVM >= 9
    //javaOptions in run += "--add-modules java.xml.bind",
    dependencyOverrides ++= Seq(
      "com.google.guava" % "guava" % "22.0",
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.typesafe.akka" %% "akka-actor" % akkaVersion
    ),
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

lazy val flow = (project in file("flow")).
  settings(
    version := "1.0-SNAPSHOT",
    scalaVersion := scalaV,
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-metrics" % akkaVersion,

      // Persistent & DData
      "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
//      "com.typesafe.akka" %% "akka-cluster-sharding" % akkaVersion,

      // Sigar library
      "io.kamon" % "sigar-loader" % "1+",

      "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test,
      "com.typesafe.akka" %% "akka-multi-node-testkit" % akkaVersion % Test,
      "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test
    )
  )
  .enablePlugins(JavaServerAppPackaging)
  .configs (MultiJvm)
  .dependsOn(shared)

lazy val shared = (project in file("shared")).
  settings(scalaVersion := scalaV)

onLoad in Global ~= (_ andThen ("project play" :: _) andThen("project flow" :: _))

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "org.wtf.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "org.wtf.binders._"