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
package domain.views

import akka.persistence.PersistentView
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

object CurrentUserTimeBookingsView {

  case class GetCurrentTimeBooking(userId: UserId)
  case object Ack

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
      val durations = state.booking.filter(_.end.isDefined).map(b => addDailyDuration(b, day)).getOrElse(getMapForDay(day))
      state = updateBooking(userId, Some(e.booking), day, durations)
      notifyClient()
      sender ! Ack
    case e: UserTimeBookingStopped =>
      log.debug(s"CurrentUserTimeBookingsView -> UserTimeBookingStopped($e.booking)")
      val day = e.booking.end.get.withTimeAtStartOfDay
      val durations = addDailyDuration(e.booking, day)
      state = updateBooking(userId, None, day, durations)
      notifyClient()
      sender ! Ack
    case e: UserTimeBookingPaused =>
      log.debug(s"CurrentUserTimeBookingsView -> UserTimeBookingPaused($e)")
      state.booking.filter(_.id == e.bookingId) map { b =>
        val newB = b.copy(end = Some(e.time))
        val day = e.time.withTimeAtStartOfDay
        val durations = getMapForDay(day)
        state = updateBooking(userId, Some(newB), day, durations)
        notifyClient()
      }
      sender ! Ack
    case e: UserTimeBookingStartTimeChanged =>
      log.debug(s"CurrentUserTimeBookingsView -> UserTimeBookingStartTimeChanged($e)")
      state.booking.filter(b => b.id == e.bookingId && b.end.isEmpty) map { b =>
        val newB = b.copy(start = e.toStart)
        state = updateBooking(userId, Some(newB), state.currentDay, state.dailyBookingsMap)
        notifyClient()
      }
      sender ! Ack
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
      sender ! Ack
    case e: UserTimeBookingRemoved =>
      e.booking.end.map { end =>
        //check if on same day        
        if (end.withTimeAtStartOfDay.isEqual(state.currentDay)) {
          if (state.booking.filter(_.id == e.booking.id).isEmpty) {
            //remove from totals
            val day = e.booking.end.get.withTimeAtStartOfDay
            val durations = removeDailyDuration(e.booking, day)
            state = updateBooking(e.booking.userId, state.booking, day, durations)
            notifyClient()
          } else if (state.booking.filter(_.id == e.booking.id).isDefined) {
            val day = e.booking.end.get.withTimeAtStartOfDay
            val durations = addDailyDuration(e.booking, day)
            state = updateBooking(e.booking.userId, None, day, durations)
            notifyClient()
          }
        }
      }.getOrElse {
        val day = DateTime.now.withTimeAtStartOfDay
        val durations = getMapForDay(day)

        state = updateBooking(e.booking.userId, None, day, durations)
        notifyClient()
      }
      sender ! Ack
    case UserTimeBookingEdited(booking, start, end) =>
      //check if on same day        
      if (end.withTimeAtStartOfDay.isEqual(state.currentDay)) {
        state.dailyBookingsMap.filter(_._1 == booking.createStub).headOption map { x =>
          //remove old booking from totals
          val day = state.currentDay
          val durations = removeDailyDuration(booking, day)
          state = updateBooking(booking.userId, state.booking, day, durations)

          //add new booking value to totals
          val newBooking = booking.copy(start = start, end = Some(end))
          val durations2 = addDailyDuration(newBooking, day)

          state = updateBooking(booking.userId, state.booking, day, durations2)
          notifyClient()
        }
        sender ! Ack
      }
    case GetCurrentTimeBooking(userId) =>
      //check if still on same day
      val day = DateTime.now.withTimeAtStartOfDay
      val durations = getMapForDay(day)
      state = updateBooking(userId, state.booking, day, durations)

      notifyClient()
      sender ! Ack
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
    val duration = calculateDuration(booking.start, booking.end.get)
    val stub = booking.createStub
    val currentDuration = currentBookings.get(stub)

    val total = currentDuration.map(_.plus(duration)).getOrElse(duration)
    currentBookings + (stub -> total)
  }

  protected def removeDailyDuration(booking: Booking, date: DateTime) = {
    val currentBookings = getMapForDay(booking.end.get.withTimeAtStartOfDay)
    val duration = calculateDuration(booking.start, booking.end.get)
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

  protected def calculateDuration(startDate: DateTime, endDate: DateTime) = {
    //split booking by dates
    val endDateTimeAtStartOfDay = endDate.withTimeAtStartOfDay
    val start = if (startDate.isBefore(endDateTimeAtStartOfDay)) endDateTimeAtStartOfDay else startDate

    //extract duration on end date    
    if (start.isAfter(endDate)) {
      log.warning(s"calculateDuration: start after enddate: $start - $endDate")
      Duration.millis(0)
    } else {
      new Interval(start, endDate).toDuration()
    }
  }

  protected def updateBooking(userId: UserId, booking: Option[Booking], currentDay: DateTime, dailyBookingsMap: Map[BookingStub, Duration]) = {
    state.copy(booking = booking, currentDay = currentDay, dailyBookingsMap = dailyBookingsMap)
  }
}