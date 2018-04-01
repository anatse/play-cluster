package org.wtf.flow

trait FlowCommand

trait DomainEvt

case class Start(flowName: String) extends FlowCommand


case class Send (processId: String, eventName:String, data:Map[String, _]) extends FlowCommand
