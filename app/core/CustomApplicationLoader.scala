/*   __                          __                                          *\
 *   / /____ ___ ____  ___  ___ _/ /       lasius                      *
 *  / __/ -_) _ `/ _ \/ _ \/ _ `/ /        contributed by tegonal              *
 *  \__/\__/\_, /\___/_//_/\_,_/_/         http://tegonal.com/                 *
 *         /___/                                                               *
 *                                                                             *
 * This program is free software: you can redistribute it and/or modify it     *
 * under the terms of the GNU General Public License as published by    *
 * the Free Software Foundation, either version 3 of the License,              *
 * or (at your option) any later version.                                      *
 *                                                                             *
 * This program is distributed in the hope that it will be useful, but         *
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY  *
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for *
 * more details.                                                               *
 *                                                                             *
 * You should have received a copy of the GNU General Public License along     *
 * with this program. If not, see http://www.gnu.org/licenses/                 *
 *                                                                             *
\*                                                                           */
package core

import play.api.Logger
import actors.{LasiusSupervisorActor, TagCache}
import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import controllers._
import domain.LoginStateAggregate
import domain.views.CurrentTeamTimeBookingsView
import models._
import play.api._
import play.api.i18n.Lang
import play.api.mvc.{ControllerComponents, RequestHeader}
import play.api.mvc.Results._
import play.api.ApplicationLoader.Context
import play.api.routing.Router
import play.controllers.AssetsComponents
import play.filters.HttpFiltersComponents
import router.Routes
import services.{TimeBookingViewService, _}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class CustomApplicationLoader extends ApplicationLoader with DefaultSystemServicesAware with ConfigAware {
  lazy val logger = Logger(getClass().getName())

  def load(context: ApplicationLoader.Context): Application = {
    //move UpdateUserId from test to app if you want to use it, see remark on UpdateUserId.update before executing
    //implicit val serialization = SerializationExtension(system)
    //UpdateUserId.update("x.y", "x.z")
    DefaultServices.getInstance()

    val initData = config.getBoolean("db.initialize_data")
    if (initData) {
      InitialData.init() map { _ =>
        systemServices.currentTeamTimeBookingsView ! CurrentTeamTimeBookingsView.Initialize
      }
    } else {
      systemServices.currentTeamTimeBookingsView ! CurrentTeamTimeBookingsView.Initialize
    }

    //initialite login handler
    LoginHandler.subscribe(systemServices.loginHandler, system.eventStream)

    //start pluginhandler
    logger.debug(s"Start pluginHandler:${systemServices.pluginHandler}")
    systemServices.pluginHandler ! PluginHandler.Startup

    new DefaultComponents(context).application
  }

  def onError(request: RequestHeader, ex: Throwable) = {
    //Airbrake.notify(request, ex)
    Future.successful(InternalServerError(views.html.errorPage(ex)(Lang.defaultLang)))
  }
}

class DefaultComponents(context: Context) extends BuiltInComponentsFromContext(context) with HttpFiltersComponents {
  lazy val currentTeamTimeBookingsController = CurrentTeamTimeBookingsController
  lazy val jsController = new JSController
  lazy val router = new Routes(httpErrorHandler,
    null,
    jsController,
    UsersController,
    null,
    TimeBookingController,
    TimeBookingHistoryController,
    CurrentUserTimeBookingsController,
    CurrentTeamTimeBookingsController,
    LatestUserTimeBookingsController,
    StructureController,
    TimeBookingStatisticsController,
    UserFavoritesController,
    null)
}

class DefaultServices extends SystemServices {
  implicit val system = ActorSystem("lasius-actor-system")

  val systemUser = UserId("lasius-system")
  val supervisor = system.actorOf(LasiusSupervisorActor.props)
  implicit val timeout = Timeout(5 seconds) // needed for `?` below
  val duration = Duration.create(5, SECONDS)
  val timeBookingViewService = Await.result(supervisor ? TimeBookingViewService.props, duration).asInstanceOf[ActorRef]

  val loginStateAggregate = Await.result(supervisor ? LoginStateAggregate.props, duration).asInstanceOf[ActorRef]
  val loginHandler =  Await.result(supervisor ? LoginHandler.props, duration).asInstanceOf[ActorRef]

  val currentUserTimeBookingsViewService = Await.result(supervisor ? CurrentUserTimeBookingsViewService.props, duration).asInstanceOf[ActorRef]
  val currentTeamTimeBookingsView = Await.result(supervisor ? CurrentTeamTimeBookingsView.props, duration).asInstanceOf[ActorRef]
  val latestUserTimeBookingsViewService = Await.result(supervisor ? LatestUserTimeBookingsViewService.props, duration).asInstanceOf[ActorRef]
  val timeBookingStatisticsViewService = Await.result(supervisor ? TimeBookingStatisticsViewService.props, duration).asInstanceOf[ActorRef]
  val tagCache = Await.result(supervisor ? TagCache.props, duration).asInstanceOf[ActorRef]
  val pluginHandler = Await.result(supervisor ? PluginHandler.props, duration).asInstanceOf[ActorRef]
}

object DefaultServices {
  private var instance: DefaultServices = null

  def getInstance() = {
    if(instance == null)
      instance = new DefaultServices()
    instance
  }
}