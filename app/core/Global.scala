package core

import models._

import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import controllers._
import play.api.mvc.RequestHeader
import scala.concurrent.Future
import akka.actor.ActorSystem
import services.TimeBookingViewService
import domain.views.CurrentUserTimeBookingsView
import services.CurrentUserTimeBookingsViewService
import services.TimeBookingHistoryViewService
import services.UserService.StartUserTimeBookingView

object Global extends GlobalSettings {

  val system = ActorSystem("lasius-actor-system")
  val executionContext = system.dispatcher
  val timeBookingManagerService = system.actorOf(TimeBookingViewService.props)

  val currentUserTimeBookingsViewService = system.actorOf(CurrentUserTimeBookingsViewService.props)
  val timeBookingHistoryViewService = system.actorOf(TimeBookingHistoryViewService.props)

  override def onStart(app: Application) {
    InitialData.init()

    Logger.debug("start persistence views")
    //TODO: start actor views when user logs in    
    timeBookingHistoryViewService ! StartUserTimeBookingView(UserId("noob"))
    currentUserTimeBookingsViewService ! domain.views.CurrentUserTimeBookingsView.GetCurrentTimeBooking(UserId("noob"))

    ()
  }

  override def onError(request: RequestHeader, ex: Throwable) = {
    //Airbrake.notify(request, ex)
    Future.successful(InternalServerError(
      views.html.errorPage(ex)))
  }
}
