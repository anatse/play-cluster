package org.wtf.flow

import akka.NotUsed
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.{EventEnvelope, PersistenceQuery}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink, Source}

import scala.concurrent.{Await, Future}
import concurrent.duration._

/**
  * Class implements process actor. This actor contains user's process information
  */
class Process(processId: String) extends Actor with ActorLogging {
  val readJournal = PersistenceQuery(context.system).readJournalFor[CassandraReadJournal](CassandraReadJournal.Identifier)

  override def receive: Receive = {
    case Start(flowName) =>
//      val flow =
//      val flowActor = FlowActor.props(flowName, processId)

    case FlowData(flowName) =>
      log.info(s"Called process: ${processId} with $flowName")

      /*
      allPersistenceIds, currentPersistenceIds
eventsByPersistenceId, currentEventsByPersistenceId
eventsByTag, currentEventsByTag
       */
      import context.dispatcher

      val snd = sender()

      implicit val mat = ActorMaterializer()
      readJournal.currentPersistenceIds()
        .take(10)
        .runFold("Ids: ")(_ + ", " + _)
        .map {
          res => log.info (s"trying to reply ${snd} with message: $res")
          snd ! res
        }

    case Events(persisenceId) =>
      log.info(s"Called get process event by id: ${processId} with $persisenceId")

      import context.dispatcher

      val snd = sender()

      implicit val mat = ActorMaterializer()
      readJournal.eventsByPersistenceId(persisenceId, 0, 4)
        .take(4)
        .runFold(Vector.empty[EventEnvelope])(_ :+ _)
        .map {
          res => log.info (s"trying to reply ${snd} with message: ${res.size}")
            snd ! res
        }

    case _ => log.info(s"Called unknown process command: ${processId}")
  }
}

object Process {
  def props(processId:String) = Props(new Process(processId))
}
