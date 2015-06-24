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
package domain.views

import akka.persistence.PersistentView
import models.UserId
import models.Booking
import akka.actor.Props
import akka.actor.ActorLogging
import akka.actor.actorRef2Scala
import domain.UserTimeBookingAggregate.UserTimeBookingAdded
import domain.UserTimeBookingAggregate.UserTimeBookingRemoved
import domain.UserTimeBookingAggregate.UserTimeBookingStarted
import domain.UserTimeBookingAggregate.UserTimeBookingStopped
import actors.ClientMessagingWebsocketActor
import models.CurrentUserTimeBooking
import repositories.BookingHistoryRepository
import repositories.UserBookingHistoryRepositoryComponent
import play.api.libs.concurrent.Execution.Implicits._
import models.UserTimeBookingHistoryEntryCleaned
import models.UserTimeBookingHistoryEntryRemoved
import models.OutEvent
import models.UserTimeBookingHistoryEntryAdded
import repositories.MongoUserBookingHistoryRepositoryComponent
import actors.ClientReceiverComponent
import actors.DefaultClientReceiverComponent

object UserTimeBookingHistoryView {

  def props(userId: UserId): Props = Props(classOf[MongoUserTimeBookingHistoryView], userId)
}

class MongoUserTimeBookingHistoryView(userId: UserId) extends UserTimeBookingHistoryView(userId)
  with MongoUserBookingHistoryRepositoryComponent with DefaultClientReceiverComponent

class UserTimeBookingHistoryView(userId: UserId) extends PersistentView with ActorLogging {
  self: UserBookingHistoryRepositoryComponent with ClientReceiverComponent =>
  import domain.UserTimeBookingAggregate._
  import domain.views.CurrentUserTimeBookingsView._

  log.debug(s"UserTimeBookingHistoryView -> created:$userId")

  override val persistenceId = userId.value
  override val viewId = userId.value + "-time-booking-history"

  val receive: Receive = {
    case e: UserTimeBookingInitialized =>
      log.debug(s"UserTimeBookingHistoryView -> initialize")
      bookingHistoryRepository.deleteByUser(userId)
      notifyClient(UserTimeBookingHistoryEntryCleaned(userId))
    case UserTimeBookingStopped(booking) =>
      log.debug(s"UserTimeBookingHistoryView -> stopped booking, add:$booking")
      bookingHistoryRepository.insert(booking)
      notifyClient(UserTimeBookingHistoryEntryAdded(booking))
    case UserTimeBookingAdded(booking) =>
      if (booking.end.isDefined) {
        log.debug(s"UserTimeBookingHistoryView -> booking added:$booking")
        bookingHistoryRepository.insert(booking)
        notifyClient(UserTimeBookingHistoryEntryAdded(booking))
      }
    case UserTimeBookingRemoved(booking) =>
      log.debug(s"UserTimeBookingHistoryView -> booking removed:$booking")
      bookingHistoryRepository.coll.remove(booking)
      notifyClient(UserTimeBookingHistoryEntryRemoved(booking.id))
  }

  private def notifyClient(event: OutEvent) = {
    clientReceiver ! (userId, event, List(userId))
  }
}