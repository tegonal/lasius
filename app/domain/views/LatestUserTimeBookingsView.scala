package domain.views

import models._
import akka.actor.Props
import akka.actor.ActorLogging
import akka.actor.actorRef2Scala
import actors.ClientMessagingWebsocketActor
import models.CurrentUserTimeBooking
import org.joda.time.Duration
import org.joda.time.DateTime
import org.joda.time.Interval
import scala.concurrent.duration._
import actors.ClientReceiverComponent
import actors.DefaultClientReceiverComponent
import akka.persistence.PersistentView
import scala.collection.SortedSet
import utils.DateTimeUtils._
import play.api.libs.json._

object LatestUserTimeBookingsView {
  case class GetLatestTimeBooking(userId: UserId, maxHistory: Int)
  case object Ack

  def props(userId: UserId): Props = Props(classOf[DefaultLatestUserTimeBookingsView], userId)
}

class DefaultLatestUserTimeBookingsView(userId: UserId)
  extends LatestUserTimeBookingsView(userId) with DefaultClientReceiverComponent {
}

class LatestUserTimeBookingsView(userId: UserId) extends PersistentView with ActorLogging {
  self: ClientReceiverComponent =>
  import domain.UserTimeBookingAggregate._
  import domain.views.LatestUserTimeBookingsView._

  override val persistenceId = userId.value
  override val viewId = userId.value + "-latest-time-bookings"

  val oldDateTime: DateTime = DateTime.parse("2000-01-01")
  val maxInternalHistory = 1000

  val ordering = Ordering.by[BookingStub, DateTime](b => getStartTime(b)).reverse

  case class TimeBookingsHistory(maxHistory: Int = maxInternalHistory,
    startTimeMap: Map[BookingStub, DateTime] = Map(),
    history: Set[BookingStub] = Set())

  var state: TimeBookingsHistory = TimeBookingsHistory()

  override def autoUpdateInterval = 100 millis

  private def getStartTime(booking: BookingStub): DateTime = {
    state.startTimeMap.get(booking).getOrElse(oldDateTime)
  }

  val receive: Receive = {
    case e: UserTimeBookingStarted =>
      addBooking(e.booking)
      sender ! Ack
    case e: UserTimeBookingStopped =>
      sender ! Ack
    case e: UserTimeBookingPaused =>
      sender ! Ack
    case e: UserTimeBookingStartTimeChanged =>
      sender ! Ack
    case e: UserTimeBookingAdded =>
      if (!state.history.isEmpty && getStartTime(state.history.last).isBefore(e.booking.start)) {
        addBooking(e.booking)
      }
    case e: UserTimeBookingRemoved =>
      sender ! Ack
    case UserTimeBookingEdited(booking, start, end) =>
      sender ! Ack
    case GetLatestTimeBooking(userId, maxHistory) =>
      state = state.copy(maxHistory = maxHistory)
      notifyClient()
      sender ! Ack
  }

  private def addBooking(booking: Booking) = {
    val stub = booking.createStub
    val newHistory = (state.history + stub).toSeq.sorted(ordering).take(maxInternalHistory).toSet
    val newStartTimeMap = state.startTimeMap + (stub -> booking.start)
    state = state.copy(history = newHistory, startTimeMap = newStartTimeMap)
    if (DateTime.now().withTimeAtStartOfDay().isBefore(booking.start)) {
      notifyClient()
    }
  }

  private def notifyClient() = {
    val result = state.history.toSeq.sorted(ordering).take(state.maxHistory)
    clientReceiver ! (userId, LatestTimeBooking(userId, result), List(userId))
  }

}