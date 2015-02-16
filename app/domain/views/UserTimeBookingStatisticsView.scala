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

      handleBooking(booking, true)
    case UserTimeBookingAdded(booking) =>
      if (booking.end.isDefined) {
        log.debug(s"UserTimeBookingHistoryView -> booking added:$booking")
        handleBooking(booking, true)
      }
    case UserTimeBookingRemoved(booking) =>
      log.debug(s"UserTimeBookingHistoryView -> booking removed:$booking")
      handleBooking(booking, false)
  }

  private def handleBooking(booking: Booking, add: Boolean) = {
    //split booking by dates
    val startDate = booking.start
    val startDateMidnight = startDate.toDateMidnight
    val endDate = booking.end.get
    val endDateMidnight = endDate.toDateMidnight

    val daysBetween = Days.daysBetween(startDateMidnight, endDateMidnight).getDays()

    //handle if start and end date are within same day
    if (daysBetween == 0) {
      val duration = endDate.minus(startDate.getMillis).toLocalDate.toInterval.toDuration
      handleDuration(booking, add, startDateMidnight, duration)
    } else {
      //extract duration at start date
      val endOfStart = endOfDay(startDate)
      val startDuration = endOfStart.minus(startDate.getMillis).toLocalDate.toInterval.toDuration
      handleDuration(booking, add, startDateMidnight, startDuration)

      //extract whole day for duration inbetween start and end date
      if (daysBetween > 1) {
        for (dayDiff <- 0 to daysBetween - 1) {
          val date = startDateMidnight.plusDays(dayDiff)

          val dayDuration = Duration.standardDays(1)
          handleDuration(booking, add, date, dayDuration)
        }
      }

      //extract duration on end date      
      val endDuration = endOfStart.minus(endDateMidnight.getMillis).toLocalDate.toInterval.toDuration
      handleDuration(booking, add, endDateMidnight, endDuration)
    }

  }

  private def endOfDay(date: DateTime): DateTime = {
    return date.withHourOfDay(23).withMinuteOfHour(59).withSecondOfMinute(59);
  }

  private def handleDuration(booking: Booking, add: Boolean, day: DateMidnight, duration: Duration) = {

  }

  private def notifyClient(event: OutEvent) = {
    ClientMessagingWebsocketActor ! (userId, event, List(userId))
  }
}