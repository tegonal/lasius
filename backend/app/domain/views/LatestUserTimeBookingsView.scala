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
import akka.stream.{ActorMaterializer, Materializer}
import domain.UserTimeBookingAggregate.UserTimeBooking
import models.UserId.UserReference
import models._
import org.joda.time.DateTime
import utils.DateTimeUtils._

import scala.annotation.nowarn
import scala.concurrent.duration._
import scala.language.postfixOps

object LatestUserTimeBookingsView {
  case class GetLatestTimeBooking(userReference: UserReference, maxHistory: Int)

  def props(clientReceiver: ClientReceiver,
            userReference: UserReference): Props =
    Props(classOf[LatestUserTimeBookingsView], clientReceiver, userReference)
}

class LatestUserTimeBookingsView(clientReceiver: ClientReceiver,
                                 userReference: UserReference)
    extends JournalReadingView
    with ActorLogging {

  import domain.views.LatestUserTimeBookingsView._

  val persistenceId: String = s"user-time-booking-${userReference.id.value}"
  // val viewId = userId.value + "-latest-time-bookings"

  val oldDateTime: DateTime = DateTime.parse("2000-01-01")
  val maxInternalHistory    = 100

  val ordering: Ordering[BookingStub] =
    Ordering.by[BookingStub, DateTime](b => getStartTime(b)).reverse

  case class TimeBookingsHistory(maxHistory: Int = maxInternalHistory,
                                 startTimeMap: Map[BookingStub, DateTime] =
                                   Map(),
                                 history: Set[BookingStub] = Set())

  var state: TimeBookingsHistory = TimeBookingsHistory()

  def autoUpdateInterval: FiniteDuration = 100 millis

  private def getStartTime(booking: BookingStub): DateTime = {
    state.startTimeMap.getOrElse(booking, oldDateTime)
  }

  override def restoreViewFromState(snapshot: UserTimeBooking): Unit = {
    snapshot.bookings.takeRight(maxInternalHistory).foreach(addBooking)
  }

  override protected val live: Receive = {
    case e: UserTimeBookingStartedV2 =>
      addBooking(e.booking)
      sender() ! Ack
    case _: UserTimeBookingStoppedV2 =>
      sender() ! Ack
    case _: UserTimeBookingPaused @nowarn("cat=deprecation") =>
      sender() ! Ack
    case _: UserTimeBookingStartTimeChanged =>
      sender() ! Ack
    case e: UserTimeBookingAddedV2 =>
      if (state.history.nonEmpty && getStartTime(state.history.last)
          .isBefore(e.booking.start.toDateTime())) {
        addBooking(e.booking)
      }
    case UserTimeBookingRemoved =>
      sender() ! Ack
    case UserTimeBookingEditedV3(_, _) =>
      sender() ! Ack
    case GetLatestTimeBooking(_, maxHistory) =>
      state = state.copy(maxHistory = maxHistory)
      notifyClient()
      sender() ! Ack
  }

  private def addBooking(booking: BookingV2): Unit = {
    val stub = booking.createStub
    val newHistory = (state.history + stub).toSeq
      .sorted(ordering)
      .take(maxInternalHistory)
      .toSet
    val newStartTimeMap =
      state.startTimeMap + (stub -> booking.start.toDateTime())
    state = state.copy(history = newHistory, startTimeMap = newStartTimeMap)
    if (DateTime
        .now()
        .withTimeAtStartOfDay()
        .isBefore(booking.start.toDateTime())) {
      notifyClient()
    }
  }

  private def notifyClient(): Unit = {
    val result = state.history.toSeq.sorted(ordering).take(state.maxHistory)
    clientReceiver ! (userReference.id, LatestTimeBooking(userReference.id,
                                                          result), List(
      userReference.id))
  }

}
