package domain.views

import akka.persistence.PersistentView
import models._
import akka.actor.Props
import akka.actor.ActorLogging
import akka.actor.actorRef2Scala
import domain.UserTimeBookingAggregate.UserTimeBookingAdded
import domain.UserTimeBookingAggregate.UserTimeBookingRemoved
import domain.UserTimeBookingAggregate.UserTimeBookingStarted
import domain.UserTimeBookingAggregate.UserTimeBookingStopped
import actors.ClientMessagingWebsocketActor
import models.CurrentUserTimeBooking
import org.joda.time.Duration
import org.joda.time.DateTime
import org.joda.time.Interval
import scala.concurrent.duration._
import actors.ClientReceiverComponent
import actors.DefaultClientReceiverComponent

object CurrentUserTimeBookingsView {

  case class GetCurrentTimeBooking(userId: UserId)

  def props(userId: UserId): Props = Props(classOf[DefaultCurrentUserTimeBookingsView], userId)
}

class DefaultCurrentUserTimeBookingsView(userId: UserId)
  extends CurrentUserTimeBookingsView(userId) with DefaultClientReceiverComponent {
}

class CurrentUserTimeBookingsView(userId: UserId) extends PersistentView with ActorLogging {
  self: ClientReceiverComponent =>
  import domain.UserTimeBookingAggregate._
  import domain.views.CurrentUserTimeBookingsView._

  override val persistenceId = userId.value
  override val viewId = userId.value + "-current-time-bookings"

  case class CurrentTimeBookings(booking: Option[Booking], currentDay: DateTime, dailyBookingsMap: Map[BookingStub, Duration])
  import domain.UserTimeBookingAggregate._

  var state: CurrentTimeBookings = CurrentTimeBookings(None, DateTime.now, Map())

  override def autoUpdateInterval = 100 millis

  val receive: Receive = {
    case e: UserTimeBookingStarted =>
      log.debug(s"CurrentUserTimeBookingsView -> UserTimeBookingStarted($e.booking)")
      val day = e.booking.start.withTimeAtStartOfDay
      val durations = getMapForDay(day)
      state = updateBooking(userId, Some(e.booking), day, durations)
      notifyClient()
    case e: UserTimeBookingStopped =>
      log.debug(s"CurrentUserTimeBookingsView -> UserTimeBookingStopped($e.booking)")
      val day = e.booking.end.get.withTimeAtStartOfDay
      val durations = addDailyDuration(e.booking, day)
      state = updateBooking(userId, None, day, durations)
      notifyClient()
    case e: UserTimeBookingAdded =>
      e.booking.end.map { end =>
        //check if on same day        
        if (end.withTimeAtStartOfDay.isEqual(state.currentDay)) {
          //add to totals
          val day = e.booking.end.get.withTimeAtStartOfDay
          val durations = addDailyDuration(e.booking, day)
          state = updateBooking(userId, None, day, durations)
        }
      }.getOrElse {
        //no end defined, still in progress
        val day = DateTime.now.withTimeAtStartOfDay
        val durations = getMapForDay(day)

        state = updateBooking(e.booking.userId, Some(e.booking), day, durations)
        notifyClient()
      }
    case e: UserTimeBookingRemoved =>
      e.booking.end.map { end =>
        //check if on same day        
        if (end.withTimeAtStartOfDay.isEqual(state.currentDay)) {
          //remove from totals
          val day = e.booking.end.get.withTimeAtStartOfDay
          val durations = removeDailyDuration(e.booking, day)
          state = updateBooking(e.booking.userId, state.booking, day, durations)
          notifyClient()
        }
      }.getOrElse {
        val day = DateTime.now.withTimeAtStartOfDay
        val durations = getMapForDay(day)

        state = updateBooking(e.booking.userId, None, day, durations)
        notifyClient()
      }
    case GetCurrentTimeBooking(userId) =>
      //check if still on same day
      val day = DateTime.now.withTimeAtStartOfDay
      val durations = getMapForDay(day)
      state = updateBooking(userId, state.booking, day, durations)

      notifyClient()
  }

  private def notifyClient() = {
    val totalBySameBooking = state.booking.map { b =>
      state.dailyBookingsMap.get(b.createStub)
    }.getOrElse(None)
    val dailyTotal = state.dailyBookingsMap.map(_._2).foldLeft(Duration.millis(0))((a, b) => a.plus(b))
    log.debug(s"notifyClient. userId:$userId, booking:${state.booking}, day:${state.currentDay}, bookings:${state.dailyBookingsMap}, totalByBooking:$totalBySameBooking, dailyTotal:$dailyTotal, dailyTotalMillis:${dailyTotal.getMillis}")
    clientReceiver ! (userId, CurrentUserTimeBooking(userId, state.booking, totalBySameBooking, dailyTotal), List(userId))
  }

  protected def addDailyDuration(booking: Booking, date: DateTime) = {
    val currentBookings = getMapForDay(booking.end.get.withTimeAtStartOfDay)
    val duration = calculateDuration(booking)
    val stub = booking.createStub
    val currentDuration = currentBookings.get(stub)

    val total = currentDuration.map(_.plus(duration)).getOrElse(duration)
    currentBookings + (stub -> total)
  }

  protected def removeDailyDuration(booking: Booking, date: DateTime) = {
    val currentBookings = getMapForDay(booking.end.get.withTimeAtStartOfDay)
    val duration = calculateDuration(booking)
    val stub = booking.createStub
    val currentDuration = currentBookings.get(stub)

    val total = currentDuration.map(_.minus(duration)).getOrElse(duration)
    currentBookings + (stub -> total)
  }

  protected def getMapForDay(day: DateTime): Map[BookingStub, Duration] = {
    if (day.isEqual(state.currentDay)) {
      state.dailyBookingsMap
    } else {
      Map()
    }
  }

  protected def calculateDuration(booking: Booking) = {
    //split booking by dates
    val endDate = booking.end.get
    val endDateTimeAtStartOfDay = endDate.withTimeAtStartOfDay
    val start = if (booking.start.isBefore(endDateTimeAtStartOfDay)) endDateTimeAtStartOfDay else booking.start

    //extract duration on end date      
    new Interval(start, endDate).toDuration()
  }

  protected def updateBooking(userId: UserId, booking: Option[Booking], currentDay: DateTime, dailyBookingsMap: Map[BookingStub, Duration]) = {
    state.copy(booking = booking, currentDay = currentDay, dailyBookingsMap = dailyBookingsMap)
  }
}