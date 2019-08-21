package domain.views

import actors.{ClientReceiverComponent, DefaultClientReceiverComponent}
import akka.actor.{ActorLogging, Props}
import akka.stream.ActorMaterializer
import models._
import org.joda.time.DateTime
import utils.DateTimeUtils._

import scala.concurrent.duration._

object LatestUserTimeBookingsView {
  case class GetLatestTimeBooking(userId: UserId, maxHistory: Int)
  case object Ack

  def props(userId: UserId): Props = Props(classOf[DefaultLatestUserTimeBookingsView], userId)
}

class DefaultLatestUserTimeBookingsView(userId: UserId)
extends LatestUserTimeBookingsView(userId) with DefaultClientReceiverComponent {
}

class LatestUserTimeBookingsView(userId: UserId) extends JournalReadingView with ActorLogging {
  self: ClientReceiverComponent =>
  import domain.views.LatestUserTimeBookingsView._

  implicit val materializer = ActorMaterializer()

  val persistenceId = userId.value
  // val viewId = userId.value + "-latest-time-bookings"

  val oldDateTime: DateTime = DateTime.parse("2000-01-01")
  val maxInternalHistory = 1000

  val ordering = Ordering.by[BookingStub, DateTime](b => getStartTime(b)).reverse

  case class TimeBookingsHistory(maxHistory: Int = maxInternalHistory,
  startTimeMap: Map[BookingStub, DateTime] = Map(),
  history: Set[BookingStub] = Set())

  var state: TimeBookingsHistory = TimeBookingsHistory()

  def autoUpdateInterval = 100 millis

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