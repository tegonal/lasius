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
import repositories.StructureRepository
import repositories.StructureMongoRepository
import actors.ClientMessagingWebsocketActor
import models.CurrentUserTimeBooking

object CurrentUserTimeBookingsView {

  case class GetCurrentTimeBooking(userId: UserId)

  def props(userId: UserId): Props = Props(new CurrentUserTimeBookingsView(userId))
}

class CurrentUserTimeBookingsView(userId: UserId) extends PersistentView with ActorLogging {

  import domain.UserTimeBookingAggregate._
  import domain.views.CurrentUserTimeBookingsView._

  override val persistenceId = userId.value
  override val viewId = userId.value + "-current-time-bookings"

  case class CurrentTimeBookings(booking: Option[Booking])

  var state: CurrentTimeBookings = CurrentTimeBookings(None)

  val receive: Receive = {
    case e: UserTimeBookingStarted =>
      log.debug(s"CurrentUserTimeBookingsView -> UserTimeBookingStarted($e.booking)")
      state = updateBooking(userId, Some(e.booking))
      notifyClient()
    case e: UserTimeBookingStopped =>
      log.debug(s"CurrentUserTimeBookingsView -> UserTimeBookingStopped($e.booking)")
      state = updateBooking(userId, None)
      notifyClient()
    case e: UserTimeBookingAdded =>
      if (!e.booking.end.isDefined) {
        state = updateBooking(e.booking.userId, Some(e.booking))
        notifyClient()
      }
    case e: UserTimeBookingRemoved =>
      if (!e.booking.end.isDefined) {
        state = updateBooking(e.booking.userId, None)
        notifyClient()
      }
    case GetCurrentTimeBooking(userId) =>
      notifyClient()
  }

  private def notifyClient() = {
    ClientMessagingWebsocketActor ! (userId, CurrentUserTimeBooking(userId, state.booking), List(userId))
  }

  private def updateBooking(userId: UserId, booking: Option[Booking]) = {
    state.copy(booking = booking)
  }
}