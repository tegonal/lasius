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
import akka.actor._
import domain.UserTimeBookingAggregate._
import repositories._
import actors.ClientMessagingWebsocketActor
import play.api.libs.concurrent.Execution.Implicits._
import models._
import org.joda.time.Days
import org.joda.time.DateMidnight
import org.joda.time.Duration
import org.joda.time.DateTime
import org.joda.time.LocalDate
import org.joda.time.Interval
import utils.DateTimeUtils._
import actors.ClientReceiverComponent
import actors.DefaultClientReceiverComponent
import play.api.Logger

object UserTimeBookingStatisticsView {

  def props(userId: UserId): Props = Props(classOf[MongoUserTimeBookingStatisticsView], userId)

  case object Ack
}

class MongoUserTimeBookingStatisticsView(userId: UserId) extends UserTimeBookingStatisticsView(userId)
  with MongoUserBookingStatisticsRepositoryComponent with DefaultClientReceiverComponent

class UserTimeBookingStatisticsView(userId: UserId) extends PersistentView with ActorLogging {
  self: UserBookingStatisticsRepositoryComponent with ClientReceiverComponent =>
  import domain.UserTimeBookingAggregate._
  import domain.views.UserTimeBookingStatisticsView._

  override val persistenceId = userId.value
  override val viewId = userId.value + "-time-booking-statistics"

  val receive: Receive = {
    case e: UserTimeBookingInitialized =>
      log.debug(s"UserTimeBookingStatisticsView -> initialize")
      bookingByProjectRepository.deleteByUser(userId)
      notifyClient(UserTimeBookingByProjectEntryCleaned(userId))

      bookingByCategoryRepository.deleteByUser(userId)
      notifyClient(UserTimeBookingByCategoryEntryCleaned(userId))

      bookingByTagRepository.deleteByUser(userId)
      notifyClient(UserTimeBookingByTagEntryCleaned(userId))
      sender ! Ack
    case UserTimeBookingStopped(booking) =>
      handleBookingAddedOrStopped(booking)
    case UserTimeBookingAdded(booking) =>
      handleBookingAddedOrStopped(booking)
    case UserTimeBookingRemoved(booking) =>
      log.debug(s"UserTimeBookingStatisticsViews -> booking removed:$booking")
      val durations = calculatDurations(booking)
      removeDurations(durations)
      val events = getEventsDurations(durations, false)
      notifyClient(events)
      sender ! Ack
  }

  protected def handleBookingAddedOrStopped(booking: Booking) = {
    if (booking.end.isDefined) {
      log.debug(s"UserTimeBookingStatisticsView -> handleBookingAddedOrStopped:$booking")
      val durations = calculatDurations(booking)
      storeDurations(durations)
      val events = getEventsDurations(durations, true)
      notifyClient(events)
    }
    sender ! Ack
  }

  protected def storeDurations(durations: Seq[OperatorEntity[_, _]]) = {
    durations map {
      _ match {
        case b: BookingByCategory =>
          bookingByCategoryRepository.add(b)
        case b: BookingByProject =>
          bookingByProjectRepository.add(b)
        case b: BookingByTag =>
          bookingByTagRepository.add(b)
        case b @ _ =>
          log.warning(s"Unsupported duration:$b")
      }
    }
  }

  protected def removeDurations(durations: Seq[OperatorEntity[_, _]]) = {
    durations map {
      _ match {
        case b: BookingByCategory =>
          bookingByCategoryRepository.subtract(b)
        case b: BookingByProject =>
          bookingByProjectRepository.subtract(b)
        case b: BookingByTag =>
          bookingByTagRepository.subtract(b)
        case b @ _ =>
          log.warning(s"Unsupported duration:$b")
      }
    }
  }

  protected def getEventsDurations(durations: Seq[_], add: Boolean): Seq[OutEvent] = {
    if (add) {
      durations map {
        _ match {
          case b: BookingByCategory =>
            Some(UserTimeBookingByCategoryEntryAdded(b))
          case b: BookingByProject =>
            Some(UserTimeBookingByProjectEntryAdded(b))
          case b: BookingByTag =>
            Some(UserTimeBookingByTagEntryAdded(b))
          case _ => None
        }
      } flatten
    } else {
      durations map {
        _ match {
          case b: BookingByCategory =>
            Some(UserTimeBookingByCategoryEntryRemoved(b))
          case b: BookingByProject =>
            Some(UserTimeBookingByProjectEntryRemoved(b))
          case b: BookingByTag =>
            Some(UserTimeBookingByTagEntryRemoved(b))
          case _ => None
        }
      } flatten
    }
  }

  protected def calculatDurations(booking: Booking): Seq[OperatorEntity[_, _]] = {
    //split booking by dates
    val startDate = booking.start
    val startDateStartOfDay = startDate.withTimeAtStartOfDay
    val endDate = booking.end.get
    val endDateStartOfDay = endDate.withTimeAtStartOfDay

    val daysBetween = Days.daysBetween(startDateStartOfDay, endDateStartOfDay).getDays()

    //handle if start and end date are within same day
    if (daysBetween == 0) {
      val duration = new Interval(startDate, endDate).toDuration()
      getDurations(booking, startDateStartOfDay, duration)
    } else {
      //extract duration at start date
      val startDuration = Duration.standardDays(1).minus(new Interval(startDateStartOfDay, startDate).toDuration())

      val startDurations = getDurations(booking, startDateStartOfDay, startDuration)

      //extract whole day for duration inbetween start and end date
      val inBetweenDurations = if (daysBetween > 1) {
        for {
          dayDiff <- 1 to daysBetween - 1
        } yield {
          val date = startDateStartOfDay.plusDays(dayDiff)

          val dayDuration = Duration.standardDays(1)
          getDurations(booking, date, dayDuration)
        }
      } else {
        Seq()
      }

      //extract duration on end date      
      val endDuration = new Interval(endDateStartOfDay, endDate).toDuration()
      val endDurations = getDurations(booking, endDateStartOfDay, endDuration)

      (startDurations ++ inBetweenDurations.flatten) ++ endDurations
    }

  }

  private def getDurations(booking: Booking, day: DateTime, duration: Duration): Seq[OperatorEntity[_, _]] = {
    Seq(BookingByCategory(BookingByCategoryId(), booking.userId, day, booking.categoryId, duration),
      BookingByProject(BookingByProjectId(), booking.userId, day, booking.projectId, duration)) ++
      booking.tags.map(tagId => BookingByTag(BookingByTagId(), booking.userId, day, tagId, duration))
  }

  private def notifyClient(events: Seq[OutEvent]) = {
    events map (event => ClientMessagingWebsocketActor ! (userId, event, List(userId)))
  }

  private def notifyClient(event: OutEvent) = {
    clientReceiver ! (userId, event, List(userId))
  }
}