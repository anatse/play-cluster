package org.wtf.flow

import akka.persistence.journal.{Tagged, WriteEventAdapter}
import org.wtf.flow.DistributedFlow.DomainEvent

class FlowEventAdapter extends WriteEventAdapter {
  override def manifest(event: Any): String = ""

  override def toJournal(event: Any): Any = {
    event match {
      case e:DomainEvent => Tagged(e, Set(e.stateName))

      case _ => event
    }
  }
}
