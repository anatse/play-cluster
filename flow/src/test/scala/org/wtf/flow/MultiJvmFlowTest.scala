package org.wtf.flow

import akka.actor.Props
import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec, MultiNodeSpecCallbacks}
import akka.testkit.ImplicitSender
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

trait STMultiNodeSpec extends MultiNodeSpecCallbacks with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def beforeAll() = multiNodeSpecBeforeAll()

  override def afterAll() = multiNodeSpecAfterAll()
}


object MultiNodeSampleConfig extends MultiNodeConfig {
  val node1 = role("backend")
  val node2 = role("backend")
}

//class MultiJvmFlowTest extends MultiNodeSpec(MultiNodeSampleConfig) with STMultiNodeSpec with ImplicitSender {
//  import MultiNodeSampleConfig._
//  import DistributedFlow._
//
//  override def initialParticipants: Int = roles.size
//
//  "A MultiNodeSample" must {
//
//    "wait for all nodes to enter a barrier" in {
//      enterBarrier("startup")
//    }
//
//    "send to and receive from a remote node" in {
//      runOn(node1) {
//        enterBarrier("deployed")
//        val ponger = system.actorSelection(node(node2) / "user" / "flowregister")
//        ponger ! "ping"
//        import scala.concurrent.duration._
//        expectMsg(10.seconds, "pong")
//      }
//
//      runOn(node2) {
//        system.actorOf(Props[FlowRegister], "flowregister")
//        enterBarrier("deployed")
//      }
//
//      enterBarrier("finished")
//    }
//  }
//}
