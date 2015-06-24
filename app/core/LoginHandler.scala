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

import akka.persistence._
import akka.actor._
import services._
import domain._
import domain.LoginStateAggregate._
import models.UserId
import core.Global._
import akka.event.EventStream

object LoginHandler {

  def subscribe(ref: ActorRef, eventStream: EventStream) = {
    eventStream.subscribe(ref, classOf[UserLoggedIn])
    eventStream.subscribe(ref, classOf[UserLoggedOut])
  }

  def props: Props = Props(new LoginHandler)
}

class LoginHandler extends Actor with ActorLogging {

  import domain.UserTimeBookingAggregate._
  import domain.LoginStateAggregate._
  import services.UserService._

  val receive: Receive = {
    case UserLoggedIn(userId) =>
      handleLoggedIn(userId)
    case UserLoggedOut(userId) =>
      handleLoggedOut(userId)
  }

  def handleLoggedIn(userId: UserId) = {
    log.debug(s"user logged in:$userId, start persistentViews")
    //initialize persistentviews
    timeBookingHistoryViewService ! StartUserTimeBookingView(userId)
    currentUserTimeBookingsViewService ! domain.views.CurrentUserTimeBookingsView.GetCurrentTimeBooking(userId)
    timeBookingStatisticsViewService ! StartUserTimeBookingView(userId)
    timeBookingManagerService ! StartAggregate(userId)
  }

  def handleLoggedOut(userId: UserId) = {
    log.debug(s"user logged in:$userId, stop persistentViews")

    //kill persistentviews
    timeBookingHistoryViewService ! StopUserView(userId)
    currentUserTimeBookingsViewService ! StopUserView(userId)
    timeBookingStatisticsViewService ! StopUserView(userId)
  }
}