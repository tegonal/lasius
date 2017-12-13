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
import scala.concurrent.duration._
import scala.concurrent.Future

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

  override def autoUpdateInterval = 1 second

  val receive: Receive = {
    case e: UserTimeBookingInitialized =>
      log.debug(s"UserTimeBookingStatisticsView -> initialize")
      bookingByTagGroupRepository.deleteByUser(userId)
      notifyClient(UserTimeBookingByTagGroupEntryCleaned(userId))

      bookingByTagRepository.deleteByUser(userId)
      notifyClient(UserTimeBookingByTagEntryCleaned(userId))
      sender ! Ack
    case UserTimeBookingStopped(booking) =>
      handleBookingAddedOrStopped(booking)
    case UserTimeBookingAdded(booking) =>
      handleBookingAddedOrStopped(booking)
    case UserTimeBookingEdited(booking, start, end) =>
      handleBookingEdited(booking, start, end)
    case UserTimeBookingRemoved(booking) =>
      log.debug(s"UserTimeBookingStatisticsViews -> booking removed:$booking")
      val durations = calculatDurations(booking)
      removeDurations(durations)
      val events = getEventsDurations(durations, false)
      notifyClient(events)
      sender ! Ack
    case UserTimeBookingStartTimeChanged(bookingId, oldStart, newStart) =>
      //do nothing, booking is still in progress
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

  protected def handleBookingEdited(booking: Booking, start: DateTime, end: DateTime) = {
    //first remove durations of 'old' booking
    val durations = calculatDurations(booking)
    removeDurations(durations)
    val events = getEventsDurations(durations, false)
    notifyClient(events)

    val newBooking = booking.copy(start = start, end = Some(end))
    val durations2 = calculatDurations(newBooking)
    storeDurations(durations2)
    val events2 = getEventsDurations(durations2, true)
    notifyClient(events2)

    sender ! Ack
  }

  protected def storeDurations(durations: Future[Seq[OperatorEntity[_, _]]]) = {
    durations map { _ map {
      _ match {
        case b: BookingByTagGroup =>
          bookingByTagGroupRepository.add(b)
        case b: BookingByTag =>
          bookingByTagRepository.add(b)
        case b @ _ =>
          log.warning(s"Unsupported duration:$b")
      }
    }
    }
  }

  protected def removeDurations(durations: Future[Seq[OperatorEntity[_, _]]]) = {
    durations map { _.map {
      _ match {
        case b: BookingByTagGroup =>
          bookingByTagGroupRepository.subtract(b)
        case b: BookingByTag =>
          bookingByTagRepository.subtract(b)
        case b @ _ =>
          log.warning(s"Unsupported duration:$b")
      }
    }
    }
  }

  protected def getEventsDurations(durations: Future[Seq[_]], add: Boolean): Future[Seq[OutEvent]] = {
    if (add) {
      durations map { _ map {
        _ match {
          case b: BookingByTagGroup =>
            Some(UserTimeBookingByTagGroupEntryAdded(b))
          case b: BookingByTag =>
            Some(UserTimeBookingByTagEntryAdded(b))
          case _ => None
        }
      } flatten
      }
    } else {
      durations map { _ map {
        _ match {
          case b: BookingByTagGroup =>
            Some(UserTimeBookingByTagGroupEntryRemoved(b))
          case b: BookingByTag =>
            Some(UserTimeBookingByTagEntryRemoved(b))
          case _ => None
        }
      } flatten
      }
    }
  }

  protected def calculatDurations(booking: Booking): Future[Seq[OperatorEntity[_, _]]] = {
    //split booking by dates
    val startDate = booking.start
    val startDateStartOfDay = startDate.withTimeAtStartOfDay
    val endDate = booking.end.get
    val endDateStartOfDay = endDate.withTimeAtStartOfDay

    val daysBetween = Days.daysBetween(startDateStartOfDay, endDateStartOfDay).getDays()

    if (endDate.isBefore(startDate)) {
      Future.successful(Seq())
    } else {
      tagGroupRepository.findByTags(booking.tags) map { tagGroups =>
        //handle if start and end date are within same day
        if (daysBetween == 0) {
          val duration = new Interval(startDate, endDate).toDuration()
          getDurations(booking, startDateStartOfDay, duration, tagGroups)
        } else {
          //extract duration at start date
          val startDuration = Duration.standardDays(1).minus(new Interval(startDateStartOfDay, startDate).toDuration())
  
          val startDurations = getDurations(booking, startDateStartOfDay, startDuration, tagGroups) 
  
            //extract whole day for duration inbetween start and end date
            val inBetweenDurations = if (daysBetween > 1) {
              for {
                dayDiff <- 1 to daysBetween - 1
              } yield {
                val date = startDateStartOfDay.plusDays(dayDiff)
    
                val dayDuration = Duration.standardDays(1)
                getDurations(booking, date, dayDuration, tagGroups)
              }
            } else {
              Seq()
            } 
            //extract duration on end date      
            val endDuration = new Interval(endDateStartOfDay, endDate).toDuration()
            val endDurations = getDurations(booking, endDateStartOfDay, endDuration, tagGroups) 
            (startDurations ++ inBetweenDurations.flatten) ++ endDurations
        }
      }

    }
  }

  private def getDurations(booking: Booking, day: DateTime, duration: Duration, tagGroups: Traversable[TagGroup]): Seq[OperatorEntity[_, _]] = {
    val bookingsByTag = booking.tags.map(tagId => BookingByTag(BookingByTagId(), booking.userId, day, tagId, duration)).toSeq
    val bookingsByTagGroup = tagGroups.map ( group => BookingByTagGroup(BookingByTagGroupId(), booking.userId, day, group.id, duration))
    bookingsByTag ++ bookingsByTagGroup
  }

  private def notifyClient(events: Future[Seq[OutEvent]]) = {
    events map (_ map (event => ClientMessagingWebsocketActor ! (userId, event, List(userId))))    
  }

  private def notifyClient(event: OutEvent) = {
    clientReceiver ! (userId, event, List(userId))
  }
}