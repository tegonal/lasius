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
import services.TimeBookingViewService
import domain.views.CurrentUserTimeBookingsView
import services._
import domain.LoginStateAggregate

object Global extends WithFilters(new play.modules.statsd.api.StatsdFilter()) with GlobalSettings  {

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
