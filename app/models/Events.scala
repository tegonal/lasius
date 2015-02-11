package models

import play.api.libs.json._

import play.api.mvc.WebSocket.FrameFormatter
import reactivemongo.bson.BSONObjectID
import julienrf.variants.Variants

sealed trait InEvent

case class HelloServer(client: String) extends InEvent

object InEvent {
  implicit val inEventFormat: Format[InEvent] = Variants.format[InEvent]("type")
}

sealed trait OutEvent
case object HelloClient extends OutEvent
case class UserLoggedOut(userId: UserId) extends OutEvent
case class CurrentUserTimeBooking(userId: UserId, booking: Option[Booking]) extends OutEvent

case class UserTimeBookingHistoryEntryCleaned(userId: UserId) extends OutEvent
case class UserTimeBookingHistoryEntryAdded(booking: Booking) extends OutEvent
case class UserTimeBookingHistoryEntryRemoved(bookingId: BookingId) extends OutEvent

object OutEvent {
  implicit val outEventFormat: Format[OutEvent] = Variants.format[OutEvent]("type")
}

object Events {
  implicit val inEventFrameFormatter = FrameFormatter.jsonFrame[InEvent]
  implicit val outEventFrameFormatter = FrameFormatter.jsonFrame[OutEvent]
}
