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

  case class LatestBookingStub(start: DateTime, categoryId: CategoryId, projectId: ProjectId, tags: Seq[TagId])
  object LatestBookingStub {
    implicit val bookingStubFormat: Format[LatestBookingStub] = Json.format[LatestBookingStub]
  }

  implicit def booking2LatestBookingStub(b: Booking): LatestBookingStub = LatestBookingStub(b.start, b.categoryId, b.projectId, b.tags)
  implicit def latestBookingStub2BookingStub(b: LatestBookingStub): BookingStub = BookingStub(b.categoryId, b.projectId, b.tags)

  case class TimeBookingsHistory(maxHistory: Int = maxHistory, history: SortedSet[LatestBookingStub] = SortedSet.empty(Ordering.by[LatestBookingStub, DateTime](_.start).reverse))
  import domain.UserTimeBookingAggregate._

  val maxHistory = 1000

  var state: TimeBookingsHistory = TimeBookingsHistory()

  override def autoUpdateInterval = 100 millis

  val receive: Receive = {
    case e: UserTimeBookingStarted =>
      val newHistory = (state.history + e.booking).take(maxHistory)
      state = state.copy(history = newHistory)
      notifyClient()
      sender ! Ack
    case e: UserTimeBookingStopped =>
      sender ! Ack
    case e: UserTimeBookingPaused =>
      sender ! Ack
    case e: UserTimeBookingStartTimeChanged =>
      sender ! Ack
    case e: UserTimeBookingAdded =>
      if (state.history.last.start.isBefore(e.booking.start)) {
        val newHistory = (state.history + e.booking).take(maxHistory)
        state = state.copy(history = newHistory)
        notifyClient()
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

  private def notifyClient() = {
    val result: Seq[BookingStub] = state.history.take(state.maxHistory).toSeq.map(latestBookingStub2BookingStub(_))
    clientReceiver ! (userId, LatestTimeBooking(userId, result), List(userId))
  }

}