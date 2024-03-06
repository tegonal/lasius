/*
 *
 * Lasius - Open source time tracker for teams
 * Copyright (c) Tegonal Genossenschaft (https://tegonal.com)
 *
 * This file is part of Lasius.
 *
 * Lasius is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * Lasius is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Lasius. If not, see <https://www.gnu.org/licenses/>.
 */

package domain.views

import actors.ClientReceiver
import akka.actor.{ActorLogging, Props}
import akka.pattern.StatusReply.Ack
import domain.UserTimeBookingAggregate.UserTimeBooking
import models.LocalDateTimeWithTimeZone.DateTimeHelper
import models.UserId.UserReference
import models._
import org.joda.time._

import scala.language.postfixOps

object CurrentUserTimeBookingsView {
  case class CurrentTimeBookings(booking: Option[UserTimeBookingStartedV3],
                                 currentDay: LocalDate,
                                 dailyBookingsMap: Map[BookingStub, Duration])

  case class GetCurrentTimeBooking(userReference: UserReference)
  case class InitializeCurrentTimeBooking(state: CurrentTimeBookings)

  def props(clientReceiver: ClientReceiver,
            userReference: UserReference): Props =
    Props(new CurrentUserTimeBookingsView(clientReceiver, userReference))
}

class CurrentUserTimeBookingsView(val clientReceiver: ClientReceiver,
                                  userReference: UserReference)
    extends JournalReadingView
    with ActorLogging {

  import domain.views.CurrentUserTimeBookingsView._

  val persistenceId: String = s"user-time-booking-${userReference.id.value}"

  var state: CurrentTimeBookings =
    CurrentTimeBookings(None, LocalDate.now, Map())

  override val receive: Receive = ({ case InitializeCurrentTimeBooking(state) =>
    this.state = state
    context.become(live)
    sender() ! JournalReadingViewIsLive
  }: Receive).orElse(defaultReceive)

  override def restoreViewFromState(snapshot: UserTimeBooking): Unit = {
    val today = LocalDate.now()

    snapshot.bookings
      .filter(b => b.start.dateTime.toLocalDate == today)
      .foreach(addDailyDuration)
    state =
      updateBooking(snapshot.bookingInProgress, today, state.dailyBookingsMap)
  }

  override protected val live: Receive = {
    case e: UserTimeBookingStartedV3 =>
      log.debug(
        s"CurrentUserTimeBookingsView -> UserTimeBookingStarted($e.booking)")
      val day       = e.start.dateTime.toLocalDate
      val durations = getMapForDay(day)
      state = updateBooking(Some(e), day, durations)

      notifyClient()
      sender() ! Ack
    case e: UserTimeBookingStoppedV3 =>
      e.booking.end.foreach { end =>
        log.debug(
          s"CurrentUserTimeBookingsView -> UserTimeBookingStopped($e.booking)")
        val day       = end.dateTime.toLocalDate
        val durations = addDailyDuration(e.booking)
        state = updateBooking(None, day, durations)
        notifyClient()
      }
      sender() ! Ack
    case e: UserTimeBookingStartTimeChanged =>
      log.debug(
        s"CurrentUserTimeBookingsView -> UserTimeBookingStartTimeChanged($e)")
      state.booking
        .find(b => b.id == e.bookingId)
        .foreach { b =>
          val newB = b.copy(start = e.toStart.toLocalDateTimeWithZone)
          state =
            updateBooking(Some(newB), state.currentDay, state.dailyBookingsMap)
          notifyClient()
        }
      sender() ! Ack
    case e: UserTimeBookingAddedV3 =>
      val booking = e.toBooking
      val day     = booking.day

      // check if on same day
      if (day.isEqual(state.currentDay)) {
        // add to totals
        val booking   = e.toBooking
        val durations = addDailyDuration(booking)
        state = updateBooking(state.booking, day, durations)
      }
      sender() ! Ack
    case e: UserTimeBookingRemovedV3 =>
      val day = e.booking.day
      // check if on same day
      if (day.isEqual(state.currentDay)) {
        if (!state.booking.exists(_.id == e.booking.id)) {
          // not current booking, remove from totals
          val durations = removeDailyDuration(e.booking)
          state = updateBooking(state.booking, day, durations)
          notifyClient()
        } else if (state.booking.exists(_.id == e.booking.id)) {
          val durations = addDailyDuration(e.booking)
          state = updateBooking(None, day, durations)
          notifyClient()
        }
      }
      sender() ! Ack
    case UserTimeBookingInProgressEdited(_, editedBooking) =>
      if (state.booking.exists(_.id == editedBooking.id)) {
        // update current booking
        val updatedBooking = state.booking.get.copy(
          start = editedBooking.start,
          tags = editedBooking.tags,
          projectReference = editedBooking.projectReference,
          organisationReference = editedBooking.organisationReference
        )
        state = updateBooking(Some(updatedBooking),
                              state.currentDay,
                              state.dailyBookingsMap)
        notifyClient()
      }
      sender() ! Ack
    case UserTimeBookingEditedV4(oldBooking, editedBooking) =>
      if (editedBooking.day.isEqual(state.currentDay)) {
        // same day, update daily map
        state.dailyBookingsMap
          .find(b => oldBooking.stub == b._1)
          .foreach { _ =>
            // remove old booking from totals
            val day       = state.currentDay
            val durations = removeDailyDuration(oldBooking)
            state = updateBooking(state.booking, day, durations)

            // add new booking value to totals
            val durations2 =
              addDailyDuration(editedBooking)
            state = updateBooking(state.booking, day, durations2)
            notifyClient()
          }
      }
      sender() ! Ack
    case GetCurrentTimeBooking(_) =>
      // check if still on same day
      val day = LocalDate.now
      if (!day.equals(state.currentDay)) {
        val durations = getMapForDay(day)
        state = updateBooking(state.booking, day, durations)
      }

      sender() ! currentUserTimeBookings
  }

  private def notifyClient(): Unit = {
    // only notify client if time booking concerns the same day
    val today = LocalDate.now()
    val event = currentUserTimeBookings

    if (!today.isAfter(state.currentDay)) {
      clientReceiver ! (userReference.id, event, List(userReference.id))
    }

    // publish to the event stream as well
    log.debug(
      s"CurrentOrganisationTimeBookingsView: publish to event stream $event: ${context.system}")
    context.system.eventStream.publish(event)
  }

  private def currentUserTimeBookings = {
    val totalBySameBooking = state.booking.flatMap { currentBooking =>
      state.dailyBookingsMap.get(currentBooking.stub)
    }
    val dailyTotal = state.dailyBookingsMap.values
      .foldLeft(Duration.millis(0))((a, b) => a.plus(b))
    log.debug(
      s"notifyClient. userId:${userReference.id}, booking:${state.booking}, day:${state.currentDay}, bookings:${state.dailyBookingsMap}, totalByBooking:$totalBySameBooking, dailyTotal:$dailyTotal, dailyTotalMillis:${dailyTotal.getMillis}")

    CurrentUserTimeBookingEvent(
      CurrentUserTimeBooking(userReference,
                             state.currentDay,
                             state.booking,
                             totalBySameBooking,
                             dailyTotal))
  }

  private def addDailyDuration(
      booking: BookingV3): Map[BookingStub, Duration] = {
    val stub = booking.stub
    val currentBookings = getMapForDay(
      booking.end.getOrElse(booking.start).toDateTime.toLocalDate)
    // if an end was provided, try to split to dorrect numbers per day
    val duration =
      booking.end.fold(booking.duration)(calculateDuration(booking.start, _))

    val currentDuration = currentBookings.get(stub)

    val total = currentDuration.map(_.plus(duration)).getOrElse(duration)
    currentBookings + (stub -> total)
  }

  private def removeDailyDuration(
      booking: BookingV3): Map[BookingStub, Duration] = {
    val currentBookings = getMapForDay(booking.day)
    val duration        = booking.duration
    val stub            = booking.stub
    val currentDuration = currentBookings.get(stub)

    val total = currentDuration.map(_.minus(duration)).getOrElse(duration)
    currentBookings + (stub -> total)
  }

  private def getMapForDay(day: LocalDate): Map[BookingStub, Duration] = {
    if (day.isEqual(state.currentDay)) {
      state.dailyBookingsMap
    } else {
      Map()
    }
  }

  private def calculateDuration(
      startDate: LocalDateTimeWithTimeZone,
      endDate: LocalDateTimeWithTimeZone): Duration = {
    // split booking by dates
    val endDateTimeAtStartOfDay = endDate.withTimeAtStartOfDay
    val start =
      if (startDate.dateTime.isBefore(endDateTimeAtStartOfDay))
        endDateTimeAtStartOfDay
      else startDate.dateTime

    // extract duration on end date
    if (start.isAfter(endDate.dateTime)) {
      log.warning(s"calculateDuration: start after enddate: $start - $endDate")
      Duration.millis(0)
    } else {
      new Interval(start.toDateTime, endDate.dateTime.toDateTime).toDuration
    }
  }

  protected def updateBooking(
      booking: Option[UserTimeBookingStartedV3],
      currentDay: LocalDate,
      dailyBookingsMap: Map[BookingStub, Duration]): CurrentTimeBookings = {
    state.copy(booking = booking,
               currentDay = currentDay,
               dailyBookingsMap = dailyBookingsMap)
  }
}
