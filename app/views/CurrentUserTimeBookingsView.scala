package views

import akka.persistence.PersistentView
import models.UserId
import models.Booking
import akka.actor.Props
import akka.actor.ActorLogging

object CurrentUserTimeBookingsView {

  case class GetCurrentTimeBooking(userId: UserId)
  case class CurrentUserTimeBooking(userId: UserId, booking: Option[Booking])

  def props: Props = Props(new CurrentUserTimeBookingsView)
}

class CurrentUserTimeBookingsView extends PersistentView with ActorLogging {

  import domain.UserTimeBookingAggregate._
  import views.CurrentUserTimeBookingsView._

  override val persistenceId = "current-time-bookings"
  override val viewId = "current-time-bookings"

  case class CurrentTimeBookings(bookings: Map[UserId, Option[Booking]])

  var state: CurrentTimeBookings = CurrentTimeBookings(Map())

  val receive: Receive = {
    case e: UserTimeBookingStarted =>
      log.debug(s"CurrentUserTimeBookingsView -> UserTimeBookingStarted($e.booking)")
      state = updateBooking(e.booking.userId, Some(e.booking))
    case e: UserTimeBookingStopped =>
      state = updateBooking(e.booking.userId, None)
    case e: UserTimeBookingAdded =>
      if (!e.booking.end.isDefined) {
        state = updateBooking(e.booking.userId, Some(e.booking))
      }
    case e: UserTimeBookingRemoved =>
      if (!e.booking.end.isDefined) {
        state = updateBooking(e.booking.userId, None)
      }
    case GetCurrentTimeBooking(userId) =>
      val res = state.bookings.get(userId).flatten
      log.debug(s"GetCurrentTimeBooking($userId) -> $res")
      sender ! CurrentUserTimeBooking(userId, res)
  }

  private def updateBooking(userId: UserId, booking: Option[Booking]) = {
    val newBookings = state.bookings.updated(userId, booking)
    state.copy(bookings = newBookings)
  }
}