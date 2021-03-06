package org.wtf.flow

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, FSM, PoisonPill, Props}
import akka.pattern.{ask, pipe}
import akka.persistence.fsm.PersistentFSM
import akka.persistence.fsm.PersistentFSM.Normal
import akka.util.Timeout

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.reflect.ClassTag

object DistributedFlow {
  def props(flow:Flow, processId: String) = Props(new FlowActor(flow, processId))

  class FlowRegister extends Actor with ActorLogging {
    override def receive: Receive = {
      case Start(flowName) =>
        log.info(s"Trying to find or create actor for flow: ${flowName}")
        // Loading flowName
        val flow = loadFlow(flowName)
        val processId = UUID.randomUUID().toString
        val flowActor = context.actorOf(props(flow, processId), s"flow_${flowName}_$processId")

        val parent = sender

        sender ! flowActor

//        self ! PoisonPill

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

class FlowActor(flow:Flow, processId: String) extends PersistentFSM[FlowState, MapData, DomainEvt] {
  override implicit def domainEventClassTag: ClassTag[DomainEvt] = deClassTag

  def idleTimer = {
    cancelTimer("idleTimer")
    setTimer("idleTimer", "end", 1800 second)
  }

  override def persistenceId: String = "Flow-" + processId

  val EXTERNAL_EVENT_NAME = "external"
  val externalState = FlowInternalState(EXTERNAL_EVENT_NAME, Seq.empty)

  override def applyEvent(domainEvent: DomainEvt, currentData: MapData): MapData = {
    domainEvent match {
      case de:DomainEvent => statesMap.get (de.stateName) match {
        case Some (event) => currentData ++ de.stateData
        case _ => currentData
      }
    }
  }

  startWith(flow.initialState, Map("processId" -> processId))

  // Start timer 30 minutes to dead
  idleTimer

  /**
    * Universal funtion for process all events for current flow
    * @param state initial state
    * @return processing function
    */
  def p (state:FlowState):PartialFunction[Event, State] = {
    /*
     * End event: It stops current actor whenever been called with no regard for current state
     */
    case Event("end", s) => {
      log.info (s"Perform end event")
      stop
    }

    /*
     * Return from subflow, include subflow ID and data
     * It checks subflowId, then process the result
     */
    case Event(retState:ReturnFromSubflow, data) if retState.data.get("subflow").isDefined && data.get("subflow").isDefined && retState.data("subflow") == data("subflow") => {
      log.info (s"Return from flow ${retState.data}, ${data}")
      data.get("nextState") match {
        case Some("end") => stop (Normal)

        case Some(nextState) => flow.states.find(_.identifier == nextState) match {
          case Some(ns) =>
            goto (ns) applying DomainEvent(ns.identifier, data - "subflow" - "nextState" + ("subflowData" -> retState.data))

          case _ =>
            stop (PersistentFSM.Failure("Desired state not found"))
        }

        case _ =>
          stop (PersistentFSM.Failure("nextState not defined"))
      }
    }

    /**
      * Endpoint for external call
      */
    case Event(extState:ExternalFlowEvent, data) => {
      log.info (s"Inside external flow ${extState.data}")
      val newData = data + ("subflow" -> extState.data("subflow"))
      log.info(s"newData: ${newData}")

      // Return data with
      stop replying ReturnFromSubflow (newData)
    }

    /**
      * Process all events for all states
      */
    case Event(ename:String, s) if state.events.find(ev => ev.name == ename).isDefined && s("processId") == processId => {
      log.info (s"Perform event: ${ename}")
      val (stateName, data) = state.events.find(ev => ev.name == ename).get.handler(s)
      val newState = flow.states.find(s => {s.identifier == stateName}).get
      newState match {
        case intState:FlowInternalState => goto (intState) applying DomainEvent(intState.identifier, s ++ data) replying (s"goto $intState")

        /*
        * External invocation. This flow should become to special state "external"
        */
        case extFlow:FlowExternalFlow =>
          log.info(s"calling external flow ${extFlow.externalFlowPath}")
          // Compute subflowId. It is used for correlation in callback message
          val subflowId = UUID.randomUUID().toString

          // Configure data for subflow
          // TODO add here configurable mapping
          val subflowData = s + ("subflow" -> subflowId, "nextState" -> extFlow.nextState, "subflowUrl" -> extFlow.externalFlowPath)

          // start new flow
          // TODO change code to make available calls between cluster nodes
          implicit val timeout = Timeout(5 seconds)
          import context.dispatcher

          val subflowFuture = (context.parent ? Start(extFlow.externalFlowPath)).mapTo[ActorRef]
          val subflow = Await.result(subflowFuture, 5 second)
          subflow ! ExternalFlowEvent(subflowData)

//          (context.parent ? Start(extFlow.externalFlowPath)).mapTo[ActorRef].onComplete {
//            case Success(subflow) =>
//              log.error(s"Success call subflow: ${subflow}")
//              subflow ! ExternalFlowEvent(subflowData)
//
//            case Failure(err) =>
//              log.error(s"Error calling subflow: ${err}")
//          }

          // Change state for listen response from subflow
          goto (externalState) applying DomainEvent (externalState.identifier, subflowData) replying subflow

        case _ => stay replying("Do nothing")
      }
    }
  }

  (flow.states :+ externalState).foreach (state => when (state) {
    idleTimer
    p(state)
  })

  whenUnhandled {
    case Event(e, s) ⇒
      log.warning(s"Received unhandled request '${e}' in state ${stateName}/${s}, ${stateName.events.find(ev => ev.name == e)}")
      stay replying "wrong state"
  }
}
