package org.wtf.health

case class HealthCheck ()

sealed trait HcResult{}
case class HcOk() extends HcResult
case class HcFail(msg: String) extends HcResult
