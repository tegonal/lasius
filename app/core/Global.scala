package core

import models._
import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import controllers._
import play.api.mvc.RequestHeader
import scala.concurrent.Future
import akka.actor.ActorSystem
import services.TimeBookingManagerService

object Global extends GlobalSettings {

  val system = ActorSystem("lasius-actor-system")
  val executionContext = system.dispatcher
  val timeBookingManagerService = system.actorOf(TimeBookingManagerService.props)

  override def onStart(app: Application) {
    InitialData.init()
    ()
  }

  override def onError(request: RequestHeader, ex: Throwable) = {
    //Airbrake.notify(request, ex)
    Future.successful(InternalServerError(
      views.html.errorPage(ex)))
  }
}
