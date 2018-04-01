package akka.testkit

import akka.actor.{Actor, ActorRef, ActorSystem, ActorSystemImpl, InternalActorRef, Props}
import akka.persistence.fsm.PersistentFSM
import akka.persistence.fsm.PersistentFSM.FSMState

import scala.reflect.ClassTag

class TestPersistentFSMRef[S <: FSMState, D, E, T <: Actor](
   system: ActorSystem,
   props:      Props,
   supervisor: ActorRef,
   name:       String)(implicit ev: T <:< PersistentFSM[S, D, E]) extends TestActorRef[T](system, props, supervisor, name) {

  private def fsm: T = underlyingActor

  def stateName: S = fsm.stateName
  def stateData: D = fsm.stateData
}

object TestPersistentFSMRef {
  def apply[S <: FSMState, D, E, T <: Actor: ClassTag](factory: ⇒ T, name:String)(implicit ev: T <:< PersistentFSM[S, D, E], system: ActorSystem): TestPersistentFSMRef[S, D, E, T] = {
    val impl = system.asInstanceOf[ActorSystemImpl]
    new TestPersistentFSMRef(impl, Props(factory), impl.guardian.asInstanceOf[InternalActorRef], name)
  }
}
