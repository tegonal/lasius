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
import org.joda.time.DateTime
import scala.collection.SortedSet
import play.api.libs.json.Json.JsValueWrapper
import scala.util._

sealed trait PersistetEvent extends Serializable

case object UndefinedEvent extends PersistetEvent

case class UserLoggedIn(userId: UserId) extends PersistetEvent

case class UserTimeBookingInitialized(userId: UserId) extends PersistetEvent
case class UserTimeBookingStarted(booking: Booking) extends PersistetEvent
case class UserTimeBookingStopped(booking: Booking) extends PersistetEvent
case class UserTimeBookingPaused(bookingId: BookingId, time: DateTime) extends PersistetEvent
case class UserTimeBookingRemoved(booking: Booking) extends PersistetEvent
case class UserTimeBookingAdded(booking: Booking) extends PersistetEvent
case class UserTimeBookingEdited(booking: Booking, start: DateTime, end: DateTime) extends PersistetEvent
case class UserTimeBookingStartTimeChanged(bookingId: BookingId, fromStart: DateTime, toStart: DateTime) extends PersistetEvent

sealed trait InEvent

case class HelloServer(client: String) extends InEvent

object InEvent {
  implicit val inEventFormat: Format[InEvent] = Variants.format[InEvent]("type")
}

sealed trait OutEvent
case object HelloClient extends OutEvent
case class UserLoggedOut(userId: UserId) extends OutEvent with PersistetEvent
case class CurrentUserTimeBooking(userId: UserId, day: DateTime, booking: Option[Booking], totalBySameBooking: Option[Duration], totalByDay: Duration)
case class CurrentUserTimeBookingEvent(booking: CurrentUserTimeBooking) extends OutEvent
case class CurrentTeamTimeBookings(teamId: TeamId, timeBookings: Seq[CurrentUserTimeBooking]) extends OutEvent

case class UserTimeBookingHistoryEntryCleaned(userId: UserId) extends OutEvent
case class UserTimeBookingHistoryEntryAdded(booking: Booking) extends OutEvent
case class UserTimeBookingHistoryEntryRemoved(bookingId: BookingId) extends OutEvent
case class UserTimeBookingHistoryEntryChanged(booking: Booking) extends OutEvent

case class UserTimeBookingByTagGroupEntryCleaned(userId: UserId) extends OutEvent
case class UserTimeBookingByTagEntryCleaned(userId: UserId) extends OutEvent

case class UserTimeBookingByTagGroupEntryAdded(booking: BookingByTagGroup) extends OutEvent
case class UserTimeBookingByTagEntryAdded(booking: BookingByTag) extends OutEvent

case class UserTimeBookingByTagGroupEntryRemoved(booking: BookingByTagGroup) extends OutEvent
case class UserTimeBookingByTagEntryRemoved(booking: BookingByTag) extends OutEvent

case class FavoriteAdded(userId: UserId, bookingStub: BookingStub) extends OutEvent
case class FavoriteRemoved(userId: UserId, bookingStub: BookingStub) extends OutEvent

case class LatestTimeBooking(userId: UserId, history: Seq[BookingStub]) extends OutEvent

case class TagCacheChanged(projectId: ProjectId, removed:Set[BaseTag], added:Set[BaseTag]) extends OutEvent

object OutEvent {
  implicit val currentUserTimeBookingFormat: Format[CurrentUserTimeBooking] = Json.format[CurrentUserTimeBooking]
  
  implicit val outEventFormat: Format[OutEvent] = Variants.format[OutEvent]("type")
}

object Events {
  implicit val inEventFrameFormatter = FrameFormatter.jsonFrame[InEvent]
  implicit val outEventFrameFormatter = FrameFormatter.jsonFrame[OutEvent]
}

object PersistetEvent {
  implicit val eventFormat: Format[PersistetEvent] = Variants.format[PersistetEvent]("type")
}
