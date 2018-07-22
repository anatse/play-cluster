package org.wtf

import akka.persistence.fsm.PersistentFSM
import scala.reflect.classTag

package object flow {
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
  val deClassTag = classTag[DomainEvt]
}
