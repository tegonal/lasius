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

object UserTimeBookingHistoryView {

  def props(userId: UserId): Props = Props(new MongoUserTimeBookingHistoryView(userId))
}

class MongoUserTimeBookingHistoryView(userId: UserId) extends UserTimeBookingHistoryView(userId)
  with MongoUserBookingHistoryRepositoryComponent

class UserTimeBookingHistoryView(userId: UserId) extends PersistentView with ActorLogging {
  self: UserBookingHistoryRepositoryComponent =>
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
    ClientMessagingWebsocketActor ! (userId, event, List(userId))
  }
}