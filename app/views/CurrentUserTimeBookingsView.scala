package views

import akka.persistence.PersistentView
import models.UserId
import models.Booking
import akka.actor.Props
import akka.actor.ActorLogging

object CurrentUserTimeBookingsView {

  case class GetCurrentTimeBooking(userId: UserId)
  case class CurrentUserTimeBooking(userId: UserId, booking: Option[Booking])

  def props(userId: UserId): Props = Props(new CurrentUserTimeBookingsView(userId))
}

class CurrentUserTimeBookingsView(userId: UserId) extends PersistentView with ActorLogging {

  import domain.UserTimeBookingAggregate._
  import views.CurrentUserTimeBookingsView._

  override val persistenceId = userId.value
  override val viewId = userId.value + "-current-time-bookings"

  case class CurrentTimeBookings(booking: Option[Booking])

  var state: CurrentTimeBookings = CurrentTimeBookings(None)

  val receive: Receive = {
    case e: UserTimeBookingStarted =>
      log.debug(s"CurrentUserTimeBookingsView -> UserTimeBookingStarted($e.booking)")
      state = updateBooking(userId, Some(e.booking))
    case e: UserTimeBookingStopped =>
       log.debug(s"CurrentUserTimeBookingsView -> UserTimeBookingStopped($e.booking)")
      state = updateBooking(userId, None)
    case e: UserTimeBookingAdded =>
      if (!e.booking.end.isDefined) {
        state = updateBooking(e.booking.userId, Some(e.booking))
      }
    case e: UserTimeBookingRemoved =>
      if (!e.booking.end.isDefined) {
        state = updateBooking(e.booking.userId, None)
      }
    case GetCurrentTimeBooking(userId) =>
      val res = state.booking
      log.debug(s"GetCurrentTimeBooking($userId) -> $res:$sender")
      sender ! CurrentUserTimeBooking(userId, res)
  }

  private def updateBooking(userId: UserId, booking: Option[Booking]) = {
    state.copy(booking = booking)
  }
}