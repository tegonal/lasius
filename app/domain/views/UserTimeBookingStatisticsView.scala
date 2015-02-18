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

object UserTimeBookingStatisticsView {

  def props(userId: UserId): Props = Props(new MongoUserTimeBookingStatisticsView(userId))
}

class MongoUserTimeBookingStatisticsView(userId: UserId) extends UserTimeBookingStatisticsView(userId)
  with MongoUserBookingStatisticsRepositoryComponent

class UserTimeBookingStatisticsView(userId: UserId) extends PersistentView with ActorLogging {
  self: UserBookingStatisticsRepositoryComponent =>
  import domain.UserTimeBookingAggregate._
  import domain.views.CurrentUserTimeBookingsView._

  override val persistenceId = userId.value
  override val viewId = userId.value + "-time-booking-statistics"

  val receive: Receive = {
    case e: UserTimeBookingInitialized =>
      log.debug(s"UserTimeBookingStatisticsView -> initialize")
      bookingByProjectRepository.deleteStatistics(userId)
      notifyClient(UserTimeBookingByProjectEntryCleaned(userId))

      bookingByCategoryRepository.deleteStatistics(userId)
      notifyClient(UserTimeBookingByCategoryEntryCleaned(userId))

      bookingByTagRepository.deleteStatistics(userId)
      notifyClient(UserTimeBookingByTagEntryCleaned(userId))
    case UserTimeBookingStopped(booking) =>
      log.debug(s"UserTimeBookingStatisticsView -> stopped booking, add:$booking")

      val durations = calculatDurations(booking)
      storeDurations(durations)
      val events = getEventsDurations(durations, false)
      notifyClient(events)
    case UserTimeBookingAdded(booking) =>
      if (booking.end.isDefined) {
        log.debug(s"UserTimeBookingStatisticsView -> booking added:$booking")
        val durations = calculatDurations(booking)
        storeDurations(durations)
        val events = getEventsDurations(durations, true)
        notifyClient(events)
      }
    case UserTimeBookingRemoved(booking) =>
      log.debug(s"UserTimeBookingStatisticsViews -> booking removed:$booking")
      val durations = calculatDurations(booking)
      val events = getEventsDurations(durations, false)
      notifyClient(events)
  }

  protected def storeDurations(durations: Seq[_]) = {
    durations map {
      _ match {
        case b: BookingByCategory =>
          bookingByCategoryRepository.add(b)
        case b: BookingByProject =>
          bookingByProjectRepository.add(b)
        case b: BookingByTag =>
          bookingByTagRepository.add(b)
      }
    }
  }

  protected def removeDurations(durations: Seq[_]) = {
    durations map {
      _ match {
        case b: BookingByCategory =>
          bookingByCategoryRepository.subtract(b)
        case b: BookingByProject =>
          bookingByProjectRepository.subtract(b)
        case b: BookingByTag =>
          bookingByTagRepository.subtract(b)
      }
    }
  }

  protected def getEventsDurations(durations: Seq[_], add: Boolean) = {
    if (add) {
      durations map {
        _ match {
          case b: BookingByCategory =>
            UserTimeBookingByCategoryEntryAdded(b)
          case b: BookingByProject =>
            UserTimeBookingByProjectEntryAdded(b)
          case b: BookingByTag =>
            UserTimeBookingByTagEntryAdded(b)
        }
      }
    } else {
      durations map {
        _ match {
          case b: BookingByCategory =>
            UserTimeBookingByCategoryEntryRemoved(b)
          case b: BookingByProject =>
            UserTimeBookingByProjectEntryRemoved(b)
          case b: BookingByTag =>
            UserTimeBookingByTagEntryRemoved(b)
        }
      }
    }
  }

  protected def calculatDurations(booking: Booking) = {
    //split booking by dates
    val startDate = booking.start
    val startDateLocalDate = startOfDay(startDate)
    val endDate = booking.end.get
    val endDateLocalDate = startOfDay(endDate)

    val daysBetween = Days.daysBetween(startDateLocalDate, endDateLocalDate).getDays()

    //handle if start and end date are within same day
    if (daysBetween == 0) {
      val duration = endDate.minus(startDate.getMillis).toLocalDate.toInterval.toDuration
      getDurations(booking, startDateLocalDate.toLocalDate, duration)
    } else {
      //extract duration at start date
      val endOfStart = endOfDay(startDate)
      val startDuration = endOfStart.minus(startDate.getMillis).toLocalDate.toInterval.toDuration

      val startDurations = getDurations(booking, startDateLocalDate.toLocalDate, startDuration)

      //extract whole day for duration inbetween start and end date
      val inBetweenDurations = if (daysBetween > 1) {
        for {
          dayDiff <- 0 to daysBetween - 1
        } yield {
          val date = startDateLocalDate.plusDays(dayDiff)

          val dayDuration = Duration.standardDays(1)
          getDurations(booking, date.toLocalDate, dayDuration)
        }
      } else {
        Seq()
      }

      //extract duration on end date      
      val endDuration = endOfStart.minus(endDateLocalDate.getMillis).toLocalDate.toInterval.toDuration
      val endDurations = getDurations(booking, endDateLocalDate.toLocalDate(), endDuration)

      (startDurations ++ inBetweenDurations) ++: endDurations
    }

  }

  private def endOfDay(date: DateTime): DateTime = {
    return date.withHourOfDay(23).withMinuteOfHour(59).withSecondOfMinute(59);
  }

  private def startOfDay(date: DateTime): DateTime = {
    return date.withHourOfDay(0).withMinuteOfHour(0).withSecondOfMinute(0);
  }

  private def getDurations(booking: Booking, day: LocalDate, duration: Duration): Seq[_] = {
    Seq(BookingByCategory(BookingByCategoryId(day, booking.categoryId), duration),
      BookingByProject(BookingByProjectId(day, booking.projectId), duration)) ++ booking.tags.map(tagId => BookingByTag(BookingByTagId(day, tagId), duration))
  }

  private def notifyClient(events: Seq[OutEvent]) = {
    events map (event => ClientMessagingWebsocketActor ! (userId, event, List(userId)))
  }

  private def notifyClient(event: OutEvent) = {
    ClientMessagingWebsocketActor ! (userId, event, List(userId))
  }
}