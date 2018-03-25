package controllers

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.cluster.Cluster
import akka.routing.FromConfig
import javax.inject._
import org.wtf.flow.Start
import play.api._
import play.api.mvc._
import akka.pattern.{ask, pipe}
import akka.util.Timeout

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(cc: ControllerComponents, system: ActorSystem) extends AbstractController(cc) {

  val backend = system.actorOf(FromConfig.props(), name = "factorialBackendRouter")
  var flows = Map.empty[String, ActorRef]

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

  def callFlow(flowName: String, state: String) = Action.async {
    implicit val timeout = Timeout(5 seconds)

    flows.get(flowName) match {
      case Some(flow)  => (flow ? state).mapTo[Any].map { message =>
        Ok(s"get from ${flowName} ${message}")
      }

      case _ => (backend ? Start(flowName)).mapTo[ActorRef].flatMap { message =>
        flows += (flowName -> message)
        (message ? state).mapTo[Any].map { result =>
          Ok(s"get from ${flowName} ${result}")
        }
      }
    }
  }
}
