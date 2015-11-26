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

import models._
import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import controllers._
import play.api.mvc.RequestHeader
import scala.concurrent.Future
import akka.actor.ActorSystem
import akka.pattern.{ ask, pipe }
import services.TimeBookingViewService
import domain.views.CurrentUserTimeBookingsView
import services._
import domain.LoginStateAggregate
import akka.LasiusSupervisorActor
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import akka.actor.ActorRef
import akka.scheduler.jira.JiraTagParseScheduler
import akka.scheduler.jira.JiraTagParseScheduler.StartScheduler


object Global extends WithFilters(new play.modules.statsd.api.StatsdFilter()) with GlobalSettings {

  val system = ActorSystem("lasius-actor-system")
  val supervisor = system.actorOf(LasiusSupervisorActor.props)
  val executionContext = system.dispatcher
  implicit val timeout = Timeout(5 seconds) // needed for `?` below
  val duration = Duration.create(5, SECONDS);
  val timeBookingManagerService = Await.result(supervisor ? TimeBookingViewService.props, duration).asInstanceOf[ActorRef]

  val loginStateAggregate = Await.result(supervisor ? LoginStateAggregate.props, duration).asInstanceOf[ActorRef]
  val loginHandler =  Await.result(supervisor ? LoginHandler.props, duration).asInstanceOf[ActorRef]

  val currentUserTimeBookingsViewService = Await.result(supervisor ? CurrentUserTimeBookingsViewService.props, duration).asInstanceOf[ActorRef]
  val latestUserTimeBookingsViewService = Await.result(supervisor ? LatestUserTimeBookingsViewService.props, duration).asInstanceOf[ActorRef]
  val timeBookingStatisticsViewService = Await.result(supervisor ? TimeBookingStatisticsViewService.props, duration).asInstanceOf[ActorRef]
  val pluginHandler = Await.result(supervisor ? PluginHandler.props, duration).asInstanceOf[ActorRef]

  override def onStart(app: Application) {
    val initData = Play.current.configuration.getBoolean("db.initialize_data")
    if (initData.isDefined && initData.get) {
      InitialData.init()
    }

    //initialite login handler
    LoginHandler.subscribe(loginHandler, system.eventStream)      
    
    //start pluginhandler
    pluginHandler ! PluginHandler.Startup
    
    ()
  }

  override def onError(request: RequestHeader, ex: Throwable) = {
    //Airbrake.notify(request, ex)
    Future.successful(InternalServerError(views.html.errorPage(ex)))
  }
}
