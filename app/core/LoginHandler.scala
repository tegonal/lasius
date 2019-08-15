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

import akka.actor._
import akka.event.EventStream
import models._

object LoginHandler {

  def subscribe(ref: ActorRef, eventStream: EventStream) = {
    eventStream.subscribe(ref, classOf[UserLoggedIn])
    eventStream.subscribe(ref, classOf[UserLoggedOut])
  }

  case class InitializeUserViews(userId: UserId)

  def props: Props = Props(new LoginHandler)
}

class LoginHandler extends Actor with ActorLogging with DefaultSystemServicesAware {

  import domain.UserTimeBookingAggregate._
  import LoginHandler._
  import services.UserService._

  val receive: Receive = {
    case InitializeUserViews(userId) =>
      initializeUserViews(userId)
    case UserLoggedIn(userId) =>
      initializeUserViews(userId)
    case UserLoggedOut(userId) =>
      handleLoggedOut(userId)
  }

  def initializeUserViews(userId: UserId) = {
    log.debug(s"user logged in:$userId, start persistentViews")
    //initialize persistentviews
    systemServices.currentUserTimeBookingsViewService ! domain.views.CurrentUserTimeBookingsView.GetCurrentTimeBooking(userId)
    systemServices.latestUserTimeBookingsViewService ! domain.views.LatestUserTimeBookingsView.GetLatestTimeBooking(userId, 5)
    systemServices.timeBookingStatisticsViewService ! StartUserTimeBookingView(userId)
    systemServices.timeBookingViewService ! StartAggregate(userId)
  }

  def handleLoggedOut(userId: UserId) = {
    log.debug(s"user logged in:$userId, stop persistentViews")

    //kill persistentviews
    systemServices.currentUserTimeBookingsViewService ! StopUserView(userId)
    systemServices.latestUserTimeBookingsViewService ! StopUserView(userId)
    systemServices.timeBookingStatisticsViewService ! StopUserView(userId)
  }
}
