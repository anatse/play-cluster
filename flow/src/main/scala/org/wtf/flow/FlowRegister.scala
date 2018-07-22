package org.wtf.flow

import akka.actor.{Actor, ActorLogging, ActorNotFound, ActorRef, Props}
import akka.persistence.fsm.PersistentFSM

import scala.concurrent.Await
import scala.reflect._
import scala.concurrent.duration._

object DistributedFlow {
  val deClassTag = classTag[DomainEvt]

  type MapData = Map[String, _]

  case class FlowEvent(name: String, handler: MapData => (String, MapData))

  sealed trait FlowState extends PersistentFSM.FSMState {
    val identifier:String = ""
    val events:Seq[FlowEvent] = Seq.empty
  }

  case class DomainEvent (stateName: String, stateData: MapData) extends DomainEvt

  case class FlowInternalState(override val identifier: String, override val events: Seq[FlowEvent]) extends FlowState
  case class FlowExternalFlow(override val identifier: String, externalFlowPath: String, nextState: String) extends FlowState

  case class ExternalFlowEvent (data:MapData)
  case class ReturnFromSubflow (data:MapData)

  case class Flow(name: String, initialState: FlowState, states: Seq[FlowState])

  def props(flow:Flow, processId: String) = Props(new FlowActor(flow, processId))

  class FlowRegister extends Actor with ActorLogging {
    private def findOrCreateActor(path: String, props: Props, name: String) = {
      try {
        Await.result ( context.actorSelection(path).resolveOne(1 second), 1 second)
      } catch {
        case ex:ActorNotFound => context.actorOf(props, name)
        case _:Throwable => null
      }
    }

    override def receive: Receive = {
      case Start(flowName) =>
        log.info(s"Trying to find or create actor for flow: ${flowName}")


        // Loading flowName
        flowName.split(":") match {
          case Array(name, processId) =>
            log.info (s"Flow parts: [$name, $processId]")
            val flow = loadFlow(name)

            val flowActorName = s"flow_${name}_${processId}"
            val flowActor = findOrCreateActor (s"user/flowRegister/$flowActorName", props(flow, processId), flowActorName)

            val procName = s"process-${processId}"
            val process = findOrCreateActor (s"user/flowRegister/$procName", Process.props(processId), procName)

            // Send flowActor to sender
            sender() ! (flowActor, process)
            log.info ("sent back message")

          case _ =>
            log.info (s"Wrong message of flow name: $flowName")
            sender() ! "Error"
        }

        log.info ("sent back message")
    }
  }

  def loadFlow(flow:String): Flow = {
    val readProducts = FlowInternalState ("readProducts", Seq(FlowEvent("callExtEvent", e => ("callExt", Map.empty))))
    val initState = FlowInternalState("init", Seq(FlowEvent("readProducts", e => ("readProducts", Map("products" -> Seq("1", "2", "3"))))))
    val extState = FlowExternalFlow ("callExt", "secondFlow", "init")
    Flow("testFlow", initState, Seq(initState, readProducts, extState))
  }
}

