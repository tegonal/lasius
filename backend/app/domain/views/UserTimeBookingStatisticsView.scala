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
import akka.actor._
import akka.pattern.StatusReply.Ack
import core.{DBSupport, SystemServices}
import domain.UserTimeBookingAggregate.UserTimeBooking
import models.UserId.UserReference
import models._
import org.joda.time.{
  DateTime,
  Days,
  Duration,
  Interval,
  LocalDate,
  LocalDateTime
}
import play.modules.reactivemongo.ReactiveMongoApi
import repositories._

import scala.annotation.tailrec
import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

object UserTimeBookingStatisticsView {

  def props(clientReceiver: ClientReceiver,
            systemServices: SystemServices,
            bookingByProjectRepository: BookingByProjectRepository,
            bookingByTagRepository: BookingByTagRepository,
            userReference: UserReference,
            reactiveMongoApi: ReactiveMongoApi): Props =
    Props(
      classOf[UserTimeBookingStatisticsView],
      clientReceiver,
      systemServices,
      bookingByProjectRepository,
      bookingByTagRepository,
      userReference,
      reactiveMongoApi
    )
}

class UserTimeBookingStatisticsView(
    clientReceiver: ClientReceiver,
    systemServices: SystemServices,
    bookingByProjectRepository: BookingByProjectRepository,
    bookingByTagRepository: BookingByTagRepository,
    userReference: UserReference,
    override val reactiveMongoApi: ReactiveMongoApi)
    extends JournalReadingView
    with ActorLogging
    with DBSupport {

  override val supportTransaction: Boolean = systemServices.supportTransaction

  val persistenceId: String = s"user-time-booking-${userReference.id.value}"

  private val waitTime = 5 seconds

  private implicit val executionContext: ExecutionContextExecutor =
    context.dispatcher

  override def restoreViewFromState(snapshot: UserTimeBooking): Unit = {
    println(
      s"~~~~~~~~~~~~~~~~~ Started building STATISTICS FOR ${userReference}: ${snapshot.bookings.size}")

    val startTime = System.currentTimeMillis()

    // recalculate states
    val durations = snapshot.bookings.flatMap { booking =>
      if (booking.end.isDefined) {
        calculateDurations(booking)
      } else {
        Seq()
      }
    }

    val bookingsByProject = durations
      .filter(_.isInstanceOf[BookingByProject])
      .map(_.asInstanceOf[BookingByProject])
      .groupBy(b => (b.day, b.organisationReference, b.projectReference))
      .map { case ((day, organisationReference, projectReference), bookings) =>
        val sum = bookings.map(_.duration).reduce((l, r) => l.plus(r))
        BookingByProject(BookingByProjectId(),
                         userReference,
                         organisationReference,
                         day,
                         projectReference,
                         duration = sum)
      }
      .toSeq

    val bookingsByTag = durations
      .filter(_.isInstanceOf[BookingByTag])
      .map(_.asInstanceOf[BookingByTag])
      .groupBy(b => (b.day, b.organisationReference, b.tagId))
      .map { case ((day, organisationReference, tag), bookings) =>
        val sum = bookings.map(_.duration).reduce((l, r) => l.plus(r))
        BookingByTag(BookingByTagId(),
                     userReference,
                     organisationReference,
                     day,
                     tag,
                     duration = sum)
      }
      .toSeq

    withinTransaction { implicit dbSession =>
      for {
        _ <- bookingByProjectRepository.deleteByUserReference(userReference)
        _ <- bookingByTagRepository.deleteByUserReference(userReference)
        _ <- bookingByProjectRepository.bulkInsert(bookingsByProject.toList)
        _ <- bookingByTagRepository.bulkInsert(bookingsByTag.toList)
      } yield ()
    }.onComplete {
      case Success(_) =>
        println(
          s"~~~~~~~~~~~~~~~~~ STATISTICS FOR ${userReference} COMPLETED IN ${System
            .currentTimeMillis() - startTime}ms")
        notifyClient(UserTimeBookingByProjectEntryCleaned(userReference.id))
        notifyClient(UserTimeBookingByTagEntryCleaned(userReference.id))
      case Failure(th) =>
        println(
          s"~~~~~~~~~~~~~~~~~ STATISTICS FOR ${userReference} FAILED IN ${System
            .currentTimeMillis() - startTime}ms, failure:${th.getMessage}")
    }
  }

  override protected val live: Receive = {
    case _: UserTimeBookingInitializedV2 =>
      log.debug(s"UserTimeBookingStatisticsView -> initialize")
      sender() ! Ack
    case UserTimeBookingStoppedV2(booking) =>
      handleBookingAddedOrStopped(booking)
    case UserTimeBookingAddedV2(booking) =>
      handleBookingAddedOrStopped(booking)
    case UserTimeBookingEditedV3(oldBooking, editedBooking) =>
      handleBookingEdited(oldBooking, editedBooking)
    case UserTimeBookingRemovedV2(booking) =>
      log.debug(s"UserTimeBookingStatisticsViews -> booking removed:$booking")
      val durations = calculateDurations(booking)
      removeDurations(durations)
      val events = getEventsDurations(durations, add = false)
      notifyClient(events)
      sender() ! Ack
    case UserTimeBookingStartTimeChanged(_, _, _) =>
      // do nothing, booking is still in progress
      sender() ! Ack
  }

  protected def handleBookingAddedOrStopped(booking: BookingV2,
                                            silent: Boolean = false): Unit = {
    if (booking.end.isDefined) {
      log.debug(
        s"UserTimeBookingStatisticsView -> handleBookingAddedOrStopped:$booking")
      val durations = calculateDurations(booking)
      storeDurations(durations)
      if (!silent) {
        val events = getEventsDurations(durations, add = true)
        notifyClient(events)
      }
    }
    if (!silent) {
      sender() ! Ack
    }
  }

  protected def handleBookingEdited(oldBooking: BookingV2,
                                    editedBooking: BookingV2): Unit = {
    // first remove durations of 'old' booking
    val durations = calculateDurations(oldBooking)
    removeDurations(durations)
    val events = getEventsDurations(durations, add = false)
    notifyClient(events)

    val durations2 = calculateDurations(editedBooking)
    storeDurations(durations2)
    val events2 = getEventsDurations(durations2, add = true)
    notifyClient(events2)

    sender() ! Ack
  }

  protected def storeDurations(
      durations: Seq[OperatorEntity[_, _]]): Seq[Any] = {
    durations.map {
      case b: BookingByProject =>
        Await.ready(withDBSession()(implicit dbSession =>
                      bookingByProjectRepository.add(b)),
                    waitTime)
      case b: BookingByTag =>
        Await.ready(
          withDBSession()(implicit dbSession => bookingByTagRepository.add(b)),
          waitTime)
      case b @ _ =>
        log.warning(s"Unsupported duration:$b")
    }
  }

  protected def removeDurations(
      durations: Seq[OperatorEntity[_, _]]): Seq[Any] = {
    durations.map {
      case b: BookingByProject =>
        Await.ready(withDBSession()(implicit dbSession =>
                      bookingByProjectRepository.subtract(b)),
                    waitTime)
      case b: BookingByTag =>
        Await.ready(withDBSession()(implicit dbSession =>
                      bookingByTagRepository.subtract(b)),
                    waitTime)
      case b @ _ =>
        log.warning(s"Unsupported duration:$b")
    }
  }

  protected def getEventsDurations(durations: Seq[_],
                                   add: Boolean): Seq[OutEvent] = {
    if (add) {
      durations.flatMap(_ match {
        case b: BookingByProject =>
          Some(UserTimeBookingByProjectEntryAdded(b))
        case b: BookingByTag =>
          Some(UserTimeBookingByTagEntryAdded(b))
        case _ => None
      })
    } else {
      durations.flatMap(_ match {
        case b: BookingByProject =>
          Some(UserTimeBookingByProjectEntryRemoved(b))
        case b: BookingByTag =>
          Some(UserTimeBookingByTagEntryRemoved(b))
        case _ => None
      })
    }
  }

  protected def calculateDurations(
      booking: BookingV2): Seq[OperatorEntity[_, _]] = {
    // split booking by dates
    booking.end match {
      case None => Seq()
      case Some(end) =>
        val startDate           = booking.start.toDateTime()
        val startDateStartOfDay = startDate.toDateTime().withTimeAtStartOfDay
        val endDate             = end.toDateTime()
        val endDateStartOfDay   = endDate.toDateTime().withTimeAtStartOfDay

        val daysBetween =
          Days.daysBetween(startDateStartOfDay, endDateStartOfDay).getDays

        if (endDate.isBefore(startDate)) {
          Seq()
        } else {

          // handle if start and end date are within same day
          if (daysBetween == 0) {
            val duration = new Interval(startDate, endDate).toDuration
            getDurations(booking, startDateStartOfDay.toLocalDate, duration)
          } else {
            // extract duration at start date
            val startDuration = Duration
              .standardDays(1)
              .minus(new Interval(startDateStartOfDay, startDate).toDuration)

            val startDurations =
              getDurations(booking,
                           startDateStartOfDay.toLocalDate,
                           startDuration)

            // extract whole day for duration inbetween start and end date
            val inBetweenDurations = if (daysBetween > 1) {
              for {
                dayDiff <- 1 until daysBetween
              } yield {
                val date = startDateStartOfDay.plusDays(dayDiff)

                val dayDuration = Duration.standardDays(1)
                getDurations(booking, date.toLocalDate, dayDuration)
              }
            } else {
              Seq()
            }

            // extract duration on end date
            val endDuration =
              new Interval(endDateStartOfDay, endDate).toDuration
            val endDurations =
              getDurations(booking, endDateStartOfDay.toLocalDate, endDuration)

            (startDurations ++ inBetweenDurations.flatten) ++ endDurations
          }

        }
    }
  }

  private def extractTags(tag: Tag): Seq[TagId] = {
    tag match {
      case group: GroupedTags =>
        group.id +: group.relatedTags.flatMap(extractTags)
      case tag => Seq(tag.id)
    }
  }

  private def getDurations(booking: BookingV2,
                           day: LocalDate,
                           duration: Duration): Seq[OperatorEntity[_, _]] = {
    Seq(
      BookingByProject(BookingByProjectId(),
                       booking.userReference,
                       booking.organisationReference,
                       day,
                       booking.projectReference,
                       duration)
    ) ++
      booking.tags.flatMap(extractTags).map { tagId =>
        BookingByTag(BookingByTagId(),
                     booking.userReference,
                     booking.organisationReference,
                     day,
                     tagId,
                     duration)
      }
  }

  private def notifyClient(events: Seq[OutEvent]) = {
    events.map(event =>
      clientReceiver ! (userReference.id, event, List(userReference.id)))
  }

  private def notifyClient(event: OutEvent): Unit = {
    clientReceiver ! (userReference.id, event, List(userReference.id))
  }
}
