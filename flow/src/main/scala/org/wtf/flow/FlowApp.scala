package org.wtf.flow

import java.util.UUID

import akka.actor.{ActorSystem, Props}
import akka.cluster.Cluster
import com.typesafe.config.ConfigFactory
import org.wtf.flow.DistributedFlow._

import scala.concurrent.Await
import scala.util.Try
import scala.concurrent.duration._

object FlowApp {
  def main(args: Array[String]): Unit = {
    val port = if (args.isEmpty) "0" else args(0)

    val config = ConfigFactory.parseString(s"""
        akka.remote.netty.tcp.port=$port
        akka.remote.artery.canonical.port=$port
        """)
      .withFallback(ConfigFactory.parseString("akka.cluster.roles = [backend]"))
      .withFallback(ConfigFactory.load("application"))

    val system = ActorSystem("ClusterSystem", config)

    Cluster(system) registerOnMemberUp {
      system.actorOf(Props[FlowRegister], name = "flowRegistry")
    }

    Cluster(system).registerOnMemberRemoved {
      // exit JVM when ActorSystem has been terminated
      system.registerOnTermination(System.exit(0))
      // shut down ActorSystem
      system.terminate()

      // In case ActorSystem shutdown takes longer than 10 seconds,
      // exit the JVM forcefully anyway.
      // We must spawn a separate thread to not block current thread,
      // since that would have blocked the shutdown of the ActorSystem.
      new Thread {
        override def run(): Unit = {
          if (Try(Await.ready(system.whenTerminated, 10 seconds)).isFailure)
            System.exit(-1)
        }
      }.start()
    }
  }
}
