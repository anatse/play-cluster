package org.wtf.flow

import akka.actor.{ActorSystem}
import akka.testkit.{ImplicitSender, TestKit, TestPersistentFSMRef, TestProbe}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import akka.util.Timeout
import org.wtf.flow.DistributedFlow._

import scala.concurrent.duration._

class FlowTest extends TestKit(ActorSystem("TestFSM")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  val processId = "123456"
  var flowDef:Flow = _
  val probe = TestProbe()
  var flowActor: TestPersistentFSMRef[FlowState, MapData, DomainEvt, FlowActor] = _

  override def beforeAll = {
    val readProducts = FlowInternalState ("readProducts", Seq(FlowEvent("callExtEvent", e => ("callExt", Map.empty))))
    val initState = FlowInternalState("init", Seq(FlowEvent("readProducts", e => ("readProducts", Map("products" -> Seq("1", "2", "3"))))))
    val extState = FlowExternalFlow ("callExt", "/user/second", "init")

    flowDef = Flow("testFlow", initState, Seq(initState, readProducts, extState))
    flowActor = TestPersistentFSMRef(new FlowActor(flowDef, processId), "first")
    probe.watch(flowActor)
  }

  "An Flow actor" must  {
    implicit val timeout = Timeout(1 seconds)

    "execute event init state and move to readProducts" in {
      flowActor.stateName.identifier should be ("init")
      flowActor.stateData("processId") should be (processId)

      flowActor ! "readProducts"
      val msg = receiveOne(1 second)
      println(msg)

      flowActor.stateName.identifier should be ("readProducts")
      flowActor.stateData.keys should contain ("products")
      flowActor.stateData("processId") should be (processId)
    }

    "trying to move to unexisted state" in {
      flowActor ! "unexisted"
      expectMsg("wrong state")
      flowActor.stateName.identifier should be ("readProducts")
    }

    "kill itself after end message was sent" in {
      flowActor ! "end"
      probe.expectTerminated(flowActor)
    }
  }
}