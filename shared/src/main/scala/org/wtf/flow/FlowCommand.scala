package org.wtf.flow

trait FlowCommand

trait DomainEvt

case class ListFlows(processId: String) extends FlowCommand

case class Start(flowName: String) extends FlowCommand

case class Send (processId: String, eventName:String, data:Map[String, _]) extends FlowCommand

case class FlowData(flowName: String) extends FlowCommand

case class Events(persisenceId: String) extends FlowCommand

