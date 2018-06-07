package controllers

import java.util.regex.Pattern

import akka.actor.{Actor, ActorRef, ActorSystem, Props, Terminated}
import akka.cluster.Cluster
import akka.routing.FromConfig
import javax.inject._
import org.wtf.flow.{Events, FlowData, Start}
import play.api._
import play.api.mvc._
import akka.pattern.{ask, pipe}
import akka.util.Timeout

import scala.collection.concurrent.TrieMap
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(cc: ControllerComponents, system: ActorSystem) extends AbstractController(cc) {

  val backend = system.actorOf(FromConfig.props(), name = "flowRegistryRouter")
  val flows = TrieMap.empty[String, ActorRef]
  var process:Option[ActorRef] = None

  class FlowHelper extends Actor {
    override def receive: Receive = {
      case flowActor:ActorRef =>
        context.watch(flowActor)

      case Terminated(flowActor) =>
        println (s"Terminated flow: ${flowActor.path.name}, ${flowActor.path.toString}")
        val pattern = "([^_]+)_([^_]+)_([^_]+)".r
        val pattern(_, name, _) = flowActor.path.name

        println (s"$name")

        flows.remove(name)
        println(flows)
    }
  }

  val helper = system.actorOf(Props(new FlowHelper()))

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }

  def getInfo() = Action.async {
    implicit val timeout = Timeout(5 seconds)

    process match {
      case Some(p) =>
        println(s"process: ${p}")
        (p ? FlowData("test")).mapTo[String].map(
          msg => Ok(msg)
        )

      case _ => Future.successful(Ok("no process"))
    }
  }

  def getEvents(id: String) = Action.async {
    implicit val timeout = Timeout(5 seconds)

    process match {
      case Some(p) =>
        println(s"process: ${p}")
        (p ? Events(id)).mapTo[Any].map {
          msg =>
            println(msg)
            Ok("ok")
        }

      case _ => Future.successful(Ok("no process"))
    }
  }


  def callFlow(flowName: String, state: String) = Action.async {
    implicit val timeout = Timeout(5 seconds)

    flows.get(flowName) match {
      case Some(flow)  => println ("Flow exists")
        (flow ? state).mapTo[Any].map { message =>
        Ok(s"get from ${flowName} ${message}")
      }

      case _ =>
        println ("Flow not found, trying to create new one")
        (backend ? Start(flowName)).mapTo[(ActorRef, ActorRef)].flatMap { message =>

          println (s"message: ${message}")

          val flowActor = message._1
          val processActor = message._2

          process = Some(processActor)

          println (s"Get Flow: ${flowActor.path}")

          flows += (flowName -> flowActor)
          helper ! message

          println (s"Trying to call Flow: ${flowActor.path}")
          (flowActor ? state).mapTo[Any].map { result =>
            Ok(s"get from ${flowName} ${result}")
          }
      }
    }
  }
}
