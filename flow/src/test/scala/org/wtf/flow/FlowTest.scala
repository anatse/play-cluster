package org.wtf.flow

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestFSMRef, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.reflect.ClassTag

class FlowTest extends TestKit(ActorSystem("TestFSM")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  import org.wtf.flow.DistributedFlow._

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  val processId = "123456"
  var flowDef:Flow = _
//  var flowActor:TestFSMRef[FlowState, Map[String, _], FlowActor] = _
//  var flowActor2:TestFSMRef[FlowState, Map[String, _], FlowActor] = _
  val probe = TestProbe()

  override def beforeAll = {
    val readProducts = FlowInternalState ("readProducts", Seq(FlowEvent("callExtEvent", e => ("callExt", Map.empty))))
    val initState = FlowInternalState("init", Seq(FlowEvent("readProducts", e => ("readProducts", Map("products" -> Seq("1", "2", "3"))))))
    val extState = FlowExternalFlow ("callExt", "/user/second", "init")

    flowDef = Flow("testFlow", initState, Seq(initState, readProducts, extState))
//    flowActor2 = TestFSMRef(new FlowActor(flowDef, "secondProcess"), "second")
//    probe watch flowActor
  }


  "An flow actor" must {
    "be created for given flow" in {
      val flowActor = TestActorRef(new FlowActor(flowDef, processId), "first")
      import reflect._
      val ct:ClassTag[DomainEvt] = classTag[DomainEvt]
      println(ct)
      ct should not be(null)
    }
  }

//  "An Flow actor" must  {
//    "execute event init state and move to readProducts" in {
//      flowActor.stateName.name should be ("init")
//      flowActor.stateData("processId") should be (processId)
//
//      flowActor ! "readProducts"
//      flowActor.stateName.name should be ("readProducts")
//      flowActor.stateData.keys should contain ("products")
//      flowActor.stateData("processId") should be (processId)
//    }
//
//    "trying to move to unexisted state" in {
//      flowActor ! "unexisted"
//      expectMsg("wrong state")
//      flowActor.stateName.name should be ("readProducts")
//    }
//
//    "call external flow and return" in {
//      flowActor ! "callExtEvent"
//      flowActor.stateName.name should be ("init")
//    }
//
//    "kill itself after end message was sent" in {
//      flowActor ! "end"
//      probe.expectTerminated(flowActor)
//    }
//  }
}