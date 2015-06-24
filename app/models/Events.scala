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
package models

import play.api.libs.json._
import play.api.mvc.WebSocket.FrameFormatter
import reactivemongo.bson.BSONObjectID
import julienrf.variants.Variants
import org.joda.time.Duration
import models.BaseFormat._

sealed trait InEvent

case class HelloServer(client: String) extends InEvent

object InEvent {
  implicit val inEventFormat: Format[InEvent] = Variants.format[InEvent]("type")
}

sealed trait OutEvent
case object HelloClient extends OutEvent
case class UserLoggedOut(userId: UserId) extends OutEvent
case class CurrentUserTimeBooking(userId: UserId, booking: Option[Booking], totalBySameBooking: Option[Duration], totalByDay: Duration) extends OutEvent

case class UserTimeBookingHistoryEntryCleaned(userId: UserId) extends OutEvent
case class UserTimeBookingHistoryEntryAdded(booking: Booking) extends OutEvent
case class UserTimeBookingHistoryEntryRemoved(bookingId: BookingId) extends OutEvent

case class UserTimeBookingByProjectEntryCleaned(userId: UserId) extends OutEvent
case class UserTimeBookingByCategoryEntryCleaned(userId: UserId) extends OutEvent
case class UserTimeBookingByTagEntryCleaned(userId: UserId) extends OutEvent

case class UserTimeBookingByProjectEntryAdded(booking: BookingByProject) extends OutEvent
case class UserTimeBookingByCategoryEntryAdded(booking: BookingByCategory) extends OutEvent
case class UserTimeBookingByTagEntryAdded(booking: BookingByTag) extends OutEvent

case class UserTimeBookingByProjectEntryRemoved(booking: BookingByProject) extends OutEvent
case class UserTimeBookingByCategoryEntryRemoved(booking: BookingByCategory) extends OutEvent
case class UserTimeBookingByTagEntryRemoved(booking: BookingByTag) extends OutEvent

case class FavoriteAdded(userId: UserId, bookingStub: BookingStub) extends OutEvent
case class FavoriteRemoved(userId: UserId, bookingStub: BookingStub) extends OutEvent

object OutEvent {
  implicit val outEventFormat: Format[OutEvent] = Variants.format[OutEvent]("type")
}

object Events {
  implicit val inEventFrameFormatter = FrameFormatter.jsonFrame[InEvent]
  implicit val outEventFrameFormatter = FrameFormatter.jsonFrame[OutEvent]
}
