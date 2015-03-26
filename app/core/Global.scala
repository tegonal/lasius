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
import services._
import domain.LoginStateAggregate

object Global extends GlobalSettings {

  val system = ActorSystem("lasius-actor-system")
  val executionContext = system.dispatcher
  val timeBookingManagerService = system.actorOf(TimeBookingViewService.props)

  val loginStateAggregate = system.actorOf(LoginStateAggregate.props)
  val loginHandler = system.actorOf(LoginHandler.props)

  val currentUserTimeBookingsViewService = system.actorOf(CurrentUserTimeBookingsViewService.props)
  val timeBookingHistoryViewService = system.actorOf(TimeBookingHistoryViewService.props)
  val timeBookingStatisticsViewService = system.actorOf(TimeBookingStatisticsViewService.props)

  override def onStart(app: Application) {
    val initData = Play.current.configuration.getBoolean("db.initialize_data")
    if (initData.isDefined && initData.get) {
      InitialData.init()
    }

    //initialite login handler
    LoginHandler.subscribe(loginHandler, system.eventStream)

    ()
  }

  override def onError(request: RequestHeader, ex: Throwable) = {
    //Airbrake.notify(request, ex)
    Future.successful(InternalServerError(views.html.errorPage(ex)))
  }
}
