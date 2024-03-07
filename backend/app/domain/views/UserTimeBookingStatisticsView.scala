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
import models.OrganisationId.OrganisationReference
import models.ProjectId.ProjectReference
import models.UserId.UserReference
import models._
import org.joda.time.{Days, Duration, Interval, LocalDate}
import play.modules.reactivemongo.ReactiveMongoApi
import repositories._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.language.postfixOps
import scala.reflect.ClassTag
import scala.util.{Failure, Success}

object UserTimeBookingStatisticsView {

  def props(clientReceiver: ClientReceiver,
            systemServices: SystemServices,
            bookingByProjectRepository: BookingByProjectRepository,
            bookingByTagRepository: BookingByTagRepository,
            bookingByTypeRepository: BookingByTypeRepository,
            userReference: UserReference,
            reactiveMongoApi: ReactiveMongoApi): Props =
    Props(
      new UserTimeBookingStatisticsView(clientReceiver,
                                        systemServices,
                                        bookingByProjectRepository,
                                        bookingByTagRepository,
                                        bookingByTypeRepository,
                                        userReference,
                                        reactiveMongoApi))
}

class UserTimeBookingStatisticsView(
    clientReceiver: ClientReceiver,
    systemServices: SystemServices,
    bookingByProjectRepository: BookingByProjectRepository,
    bookingByTagRepository: BookingByTagRepository,
    bookingByTypeRepository: BookingByTypeRepository,
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
      s"~~~~~~~~~~~~~~~~~ Started building STATISTICS FOR $userReference: ${snapshot.bookings.size}")

    val startTime = System.currentTimeMillis()

    // recalculate states
    val durations = snapshot.bookings.flatMap { booking =>
      if (booking.end.isDefined) {
        calculateDurations(booking)
      } else {
        Seq()
      }
    }

    def getDurationsByType[T <: OperatorEntity[_, _], K](groupBy: T => K)(
        reducer: (K, Duration) => T)(implicit ct: ClassTag[T]): Seq[T] = {
      val clazz = ct.runtimeClass
      durations
        .filter(_.getClass.isAssignableFrom(clazz))
        .map(_.asInstanceOf[T])
        .groupBy(groupBy)
        .map { case (key, bookings) =>
          val sum = bookings.map(_.duration).reduce((l, r) => l.plus(r))
          reducer(key, sum)
        }
        .toSeq
    }

    val bookingsByProject =
      getDurationsByType[BookingByProject,
                         (LocalDate, OrganisationReference, ProjectReference)] {
        b => (b.day, b.organisationReference, b.projectReference)
      } { (key, sum) =>
        BookingByProject(_id = BookingByProjectId(),
                         userReference = userReference,
                         organisationReference = key._2,
                         day = key._1,
                         projectReference = key._3,
                         duration = sum)
      }

    val bookingsByTag =
      getDurationsByType[BookingByTag,
                         (LocalDate, OrganisationReference, TagId)] { b =>
        (b.day, b.organisationReference, b.tagId)
      } { (key, sum) =>
        BookingByTag(_id = BookingByTagId(),
                     userReference = userReference,
                     organisationReference = key._2,
                     day = key._1,
                     tagId = key._3,
                     duration = sum)
      }

    val bookingsByType =
      getDurationsByType[BookingByType,
                         (LocalDate, OrganisationReference, BookingType)] { b =>
        (b.day, b.organisationReference, b.bookingType)
      } { (key, sum) =>
        BookingByType(_id = BookingByTypeId(),
                      userReference = userReference,
                      organisationReference = key._2,
                      day = key._1,
                      bookingType = key._3,
                      duration = sum)
      }

    withinTransaction { implicit dbSession =>
      for {
        _ <- bookingByProjectRepository.deleteByUserReference(userReference)
        _ <- bookingByTagRepository.deleteByUserReference(userReference)
        _ <- bookingByProjectRepository.bulkInsert(bookingsByProject.toList)
        _ <- bookingByTagRepository.bulkInsert(bookingsByTag.toList)
        _ <- bookingByTypeRepository.bulkInsert(bookingsByType.toList)
      } yield ()
    }.onComplete {
      case Success(_) =>
        println(
          s"~~~~~~~~~~~~~~~~~ STATISTICS FOR $userReference COMPLETED IN ${System
            .currentTimeMillis() - startTime}ms")
        notifyClient(UserTimeBookingByProjectEntryCleaned(userReference.id))
        notifyClient(UserTimeBookingByTagEntryCleaned(userReference.id))
      case Failure(th) =>
        println(
          s"~~~~~~~~~~~~~~~~~ STATISTICS FOR $userReference FAILED IN ${System
            .currentTimeMillis() - startTime}ms, failure:${th.getMessage}")
    }
  }

  override protected val live: Receive = {
    case _: UserTimeBookingInitializedV2 =>
      log.debug(s"UserTimeBookingStatisticsView -> initialize")
      sender() ! Ack
    case UserTimeBookingStoppedV3(booking) =>
      handleBookingAddedOrStopped(booking)
    case e: UserTimeBookingAddedV3 =>
      handleBookingAddedOrStopped(e.toBooking)
    case UserTimeBookingEditedV4(oldBooking, editedBooking) =>
      handleBookingEdited(oldBooking, editedBooking)
    case UserTimeBookingRemovedV3(booking) =>
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

  private def handleBookingAddedOrStopped(booking: BookingV3,
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

  private def handleBookingEdited(oldBooking: BookingV3,
                                  editedBooking: BookingV3): Unit = {
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

  private def storeDurations(durations: Seq[OperatorEntity[_, _]]): Seq[Any] = {
    durations.map {
      case b: BookingByProject =>
        Await.ready(withDBSession()(implicit dbSession =>
                      bookingByProjectRepository.add(b)),
                    waitTime)
      case b: BookingByTag =>
        Await.ready(
          withDBSession()(implicit dbSession => bookingByTagRepository.add(b)),
          waitTime)
      case b: BookingByType =>
        Await.ready(
          withDBSession()(implicit dbSession => bookingByTypeRepository.add(b)),
          waitTime)
      case b @ _ =>
        log.warning(s"Unsupported duration:$b")
    }
  }

  private def removeDurations(
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
      case b: BookingByType =>
        Await.ready(withDBSession()(implicit dbSession =>
                      bookingByTypeRepository.subtract(b)),
                    waitTime)
      case b @ _ =>
        log.warning(s"Unsupported duration:$b")
    }
  }

  private def getEventsDurations(durations: Seq[_],
                                 add: Boolean): Seq[OutEvent] = {
    if (add) {
      durations.flatMap(_ match {
        case b: BookingByProject =>
          Some(UserTimeBookingByProjectEntryAdded(b))
        case b: BookingByTag =>
          Some(UserTimeBookingByTagEntryAdded(b))
        case b: BookingByType =>
          Some(UserTimeBookingByTypeEntryAdded(b))
        case _ => None
      })
    } else {
      durations.flatMap(_ match {
        case b: BookingByProject =>
          Some(UserTimeBookingByProjectEntryRemoved(b))
        case b: BookingByTag =>
          Some(UserTimeBookingByTagEntryRemoved(b))
        case b: BookingByType =>
          Some(UserTimeBookingByTypeEntryRemoved(b))
        case _ => None
      })
    }
  }

  private def calculateDurations(
      booking: BookingV3): Seq[OperatorEntity[_, _]] = {
    // split booking by dates
    booking.end match {
      case None =>
        // map all durations to start day of booking
        getDurations(booking, booking.day, booking.duration)
      case Some(end) =>
        val startDate           = booking.start.toDateTime
        val startDateStartOfDay = startDate.toDateTime.withTimeAtStartOfDay
        val endDate             = end.toDateTime
        val endDateStartOfDay   = endDate.toDateTime.withTimeAtStartOfDay

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

  private def getDurations(booking: BookingV3,
                           day: LocalDate,
                           duration: Duration): Seq[OperatorEntity[_, _]] = {
    Seq(
      BookingByType(BookingByTypeId(),
                    booking.userReference,
                    booking.organisationReference,
                    day,
                    booking.bookingType,
                    duration)
    ) ++
      booking.projectReference
        .map { projectReference =>
          Seq(
            BookingByProject(BookingByProjectId(),
                             booking.userReference,
                             booking.organisationReference,
                             day,
                             projectReference,
                             duration))
        }
        .getOrElse(Seq()) ++
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
