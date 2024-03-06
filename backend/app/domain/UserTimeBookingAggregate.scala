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

package domain

import actors.ClientReceiver
import akka.actor._
import akka.persistence._
import core.{DBSupport, SystemServices}
import models.LocalDateTimeWithTimeZone.DateTimeHelper
import models.OrganisationId.OrganisationReference
import models.ProjectId.ProjectReference
import models.UserId.UserReference
import models._
import org.joda.time.DateTime
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoApi
import repositories.BookingHistoryRepository

import scala.annotation.nowarn
import scala.concurrent.duration.{Duration, MINUTES}
import scala.concurrent.{Await, ExecutionContextExecutor}

object UserTimeBookingAggregate {

  import AggregateRoot._

  case class UserTimeBooking(
      userReference: UserReference,
      bookingInProgress: Option[UserTimeBookingStartedV3],
      bookings: Seq[BookingV3])
      extends State

  object UserTimeBooking {
    implicit val userTimeBookingFormat: Format[UserTimeBooking] =
      Json.using[Json.WithDefaultValues].format[UserTimeBooking]
  }

  private case object KillAggregate extends Command

  trait UserTimeBookingCommand extends Command {
    val userReference: UserReference
  }

  case class StartAggregate(userReference: UserReference)
      extends UserTimeBookingCommand

  case class ChangeStartTimeOfBooking(
      userReference: UserReference,
      organisationReference: OrganisationReference,
      bookingId: BookingId,
      newStart: DateTime)
      extends UserTimeBookingCommand

  case class StartProjectBookingCommand(
      userReference: UserReference,
      organisationReference: OrganisationReference,
      projectReference: ProjectReference,
      tags: Set[Tag],
      start: DateTime)
      extends UserTimeBookingCommand

  case class EndProjectBookingCommand(
      userReference: UserReference,
      organisationReference: OrganisationReference,
      bookingId: BookingId,
      end: DateTime)
      extends UserTimeBookingCommand

  case class RemoveBookingCommand(userReference: UserReference,
                                  organisationReference: OrganisationReference,
                                  bookingId: BookingId)
      extends UserTimeBookingCommand

  case class AddBookingCommand(
      bookingType: BookingType,
      userReference: UserReference,
      organisationReference: OrganisationReference,
      projectReference: Option[ProjectReference],
      tags: Set[Tag],
      start: DateTime,
      endOrDuration: Either[DateTime, org.joda.time.Duration])
      extends UserTimeBookingCommand

  case class UpdateBookingCommand(
      userReference: UserReference,
      organisationReference: OrganisationReference,
      bookingId: BookingId,
      projectReference: Option[ProjectReference],
      tags: Option[Set[Tag]],
      start: Option[DateTime],
      endOrDuration: Option[Either[DateTime, org.joda.time.Duration]])
      extends UserTimeBookingCommand

  def props(systemServices: SystemServices,
            clientReceiver: ClientReceiver,
            bookingHistoryRepository: BookingHistoryRepository,
            userReference: UserReference,
            reactiveMongoApi: ReactiveMongoApi): Props =
    Props(
      new UserTimeBookingAggregate(systemServices,
                                   clientReceiver,
                                   bookingHistoryRepository,
                                   userReference,
                                   reactiveMongoApi))
}

class UserTimeBookingAggregate(
    systemServices: SystemServices,
    clientReceiver: ClientReceiver,
    bookingHistoryRepository: BookingHistoryRepository,
    userReference: UserReference,
    override val reactiveMongoApi: ReactiveMongoApi)
    extends AggregateRoot
    with DBSupport {

  override val supportTransaction: Boolean = systemServices.supportTransaction

  import AggregateRoot._
  import UserTimeBookingAggregate._

  log.debug(s"UserTimeBookingAggregate: created ${userReference.key}")

  override def persistenceId: String =
    s"user-time-booking-${userReference.id.value}"

  override var state: State = UserTimeBooking(userReference, None, Seq())

  implicit val executionContext: ExecutionContextExecutor = context.dispatcher

  private def newBookingId: BookingId = BookingId()

  /** Updates internal processor state according to event that is to be applied.
    *
    * @param evt
    *   Event to apply
    */
  override def updateState(evt: PersistedEvent): Unit = {
    log.debug(s"updateState: $evt")
    evt match {
      case _: UserTimeBookingInitializedV2 =>
        log.debug(s"UserTimeBookingInitialized")
        context.become(created)
        notifyClient(UserTimeBookingHistoryEntryCleaned(userReference.id))
      case booking: UserTimeBookingStartedV3 =>
        log.debug(s"UserBookingStarted - $booking")
        state = state match {
          case ub: UserTimeBooking => startUserBooking(ub, booking)
          case _                   => state
        }
      case UserTimeBookingStoppedV3(booking) =>
        log.debug(s"UserBookingStopped - $booking")
        state = state match {
          case ub: UserTimeBooking if booking.end.isDefined =>
            endAndLogUserBooking(ub, booking.id, booking.end.get)
          case _ => state
        }
      case e: UserTimeBookingPaused @nowarn("cat=deprecation") =>
        log.debug(s"UserBookingPaused - ${e.bookingId}")
        state = state match {
          case ub: UserTimeBooking =>
            endAndLogUserBooking(ub,
                                 e.bookingId,
                                 e.time.toLocalDateTimeWithZone)
          case _ => state
        }
      case UserTimeBookingRemovedV3(booking) =>
        log.debug(s"UserBookingRemoved - $booking")
        state = state match {
          case ub: UserTimeBooking => removeUserBooking(ub, booking)
          case _                   => state
        }
      case e: UserTimeBookingAddedV3 =>
        val booking = e.toBooking
        log.debug(s"UserBookingAdded - $booking")
        state = state match {
          case ub: UserTimeBooking => addUserBooking(ub, booking)
          case _                   => state
        }
      case UserTimeBookingEditedV4(booking, newBooking) =>
        log.debug(s"UserBookingEdited- $booking: $newBooking")
        state = state match {
          case ub: UserTimeBooking => editUserBooking(ub, newBooking)
          case _                   => state
        }
      case UserTimeBookingInProgressEdited(booking, newBooking) =>
        log.debug(s"UserTimeBookingInProgressEdited- $booking: $newBooking")
        state = state match {
          case ub: UserTimeBooking => editUserBookingInProgress(ub, newBooking)
          case _                   => state
        }
      case UserTimeBookingStartTimeChanged(bookingId, fromStart, toStart) =>
        log.debug(
          s"UserBookingStartTimeChanged - $bookingId - $fromStart -> $toStart")
        state = state match {
          case ub: UserTimeBooking =>
            updateStartTime(ub, bookingId, toStart)
          case _ => state
        }
      case _ =>
    }
  }

  private def startUserBooking(
      ub: UserTimeBooking,
      booking: UserTimeBookingStartedV3): UserTimeBooking = {
    ub.copy(bookingInProgress = Some(booking))
  }

  private def addUserBooking(ub: UserTimeBooking,
                             booking: BookingV3): UserTimeBooking = {

    notifyClient(UserTimeBookingHistoryEntryAdded(booking))
    Await.ready(withDBSession()(implicit dbSession =>
                  bookingHistoryRepository.upsert(booking)),
                systemServices.duration)

    ub.copy(bookings = ub.bookings :+ booking)
  }

  private def endAndLogUserBooking(
      ub: UserTimeBooking,
      bookingId: BookingId,
      endTime: LocalDateTimeWithTimeZone): UserTimeBooking = {

    ub.bookingInProgress
      .flatMap { b =>
        if (b.id == bookingId) {
          val newBooking = b.toBooking(Left(endTime))

          notifyClient(UserTimeBookingHistoryEntryAdded(newBooking))
          Await.ready(withDBSession()(implicit dbSession =>
                        bookingHistoryRepository.upsert(newBooking)),
                      systemServices.duration)
          Some(
            ub.copy(bookings = ub.bookings :+ newBooking,
                    bookingInProgress = None))
        } else {
          None
        }
      }
      .getOrElse(ub)
  }

  private def removeUserBooking(ub: UserTimeBooking,
                                booking: BookingV3): UserTimeBooking = {
    notifyClient(UserTimeBookingHistoryEntryRemoved(booking.id))
    Await.ready(withDBSession()(implicit dbSession =>
                  bookingHistoryRepository.remove(booking)),
                systemServices.duration)

    val newBookings = ub.bookings.filter(_.id != booking.id)
    ub.copy(bookings = newBookings)
  }

  private def editUserBooking(ub: UserTimeBooking,
                              newBooking: BookingV3): UserTimeBooking = {
    notifyClient(UserTimeBookingHistoryEntryChanged(newBooking))
    Await.ready(
      withDBSession()(implicit dbSession =>
        bookingHistoryRepository.updateBooking(newBooking)).map {
        case true => // success
        case _    => log.warning(s"Couldn't update time booking:$newBooking")
      },
      systemServices.duration
    )

    val newBookings = ub.bookings.map { b =>
      if (b.id == newBooking.id)
        newBooking
      else b
    }
    ub.copy(bookings = newBookings)
  }

  private def editUserBookingInProgress(
      ub: UserTimeBooking,
      newBooking: UserTimeBookingStartedV3): UserTimeBooking = {
    notifyClient(UserTimeBookingInProgressEntryChanged(Some(newBooking)))

    ub.copy(bookingInProgress = Some(newBooking))
  }

  private def updateStartTime(ub: UserTimeBooking,
                              bookingId: BookingId,
                              toStart: DateTime): UserTimeBooking = {
    val updatedBooking = ub.bookingInProgress.map { b =>
      if (b.id == bookingId)
        b.copy(start = toStart.toLocalDateTimeWithZone)
      else b
    }
    notifyClient(UserTimeBookingInProgressEntryChanged(updatedBooking))
    ub.copy(bookingInProgress = updatedBooking)
  }

  override def restoreFromSnapshot(metadata: SnapshotMetadata,
                                   state: State): Unit = {
    state match {
      case Removed => context.become(removed)
      case Created => context.become(created)
      case _: User => context.become(uninitialized)
      case s: UserTimeBooking =>
        this.state = s
        Await.ready(
          withinTransaction { implicit dbSession =>
            for {
              _ <- bookingHistoryRepository.deleteByUserReference(userReference)
              _ <- bookingHistoryRepository.bulkInsert(s.bookings.toList)
            } yield ()
          },
          Duration.create(5, MINUTES)
        )
    }
  }

  override protected def afterRecoveryCompleted(sequenceNr: Long,
                                                state: State): Unit = {
    state match {
      case s: UserTimeBooking =>
        sendToUserServices(RestoreViewFromState(userReference, sequenceNr, s))
    }
  }

  override protected def publish(event: PersistedEvent): Unit = {
    super.publish(event)

    // send to view for live updates
    sendToUserServices(ForwardPersistentEvent(userReference, event))
  }

  private def sendToUserServices(message: AnyRef): Unit = {
    systemServices.currentUserTimeBookingsViewService ! message
    systemServices.timeBookingStatisticsViewService ! message
    systemServices.currentOrganisationTimeBookingsView ! message
  }

  private val uninitialized: Receive = defaultReceive.orElse {
    case GetState =>
      sender() ! state
    case Initialize(state) =>
      log.debug(s"Initialize: $state")
      this.state = state
      context.become(created)
    case e =>
      log.debug(s"InitBooking -> userId: ${userReference.key}:$e")
      persist(UserTimeBookingInitializedV2(userReference))(afterEventPersisted)
      context.become(created)
      created(e)
  }

  private val created: Receive = defaultReceive.orElse {
    case StartProjectBookingCommand(_,
                                    organisationReference,
                                    projectReference,
                                    tags,
                                    start) =>
      log.debug(
        s"StartBooking -> projectId:${projectReference.id}, tags:$tags, start:$start")
      val startTime = start.toLocalDateTimeWithZone
      // if another booking is still in progress
      state match {
        case ub: UserTimeBooking =>
          ub.bookingInProgress.foreach { b =>
            val stoppedB = b.toBooking(Left(startTime))
            persist(UserTimeBookingStoppedV3(stoppedB))(afterEventPersisted)
          }
        case _ =>
      }

      persist(
        UserTimeBookingStartedV3(
          id = newBookingId,
          bookingType = ProjectBooking,
          start = startTime,
          userReference = userReference,
          organisationReference = organisationReference,
          projectReference = Some(projectReference),
          tags = tags
        ))(afterEventPersisted)
    case EndProjectBookingCommand(userReference,
                                  organisationReference,
                                  bookingId,
                                  end) =>
      log.debug(s"EndBooking -> bookingId:$bookingId")
      state match {
        case b: UserTimeBooking =>
          b.bookingInProgress.foreach { b =>
            if (b.id == bookingId && b.userReference == userReference && b.organisationReference == organisationReference) {
              persist(
                UserTimeBookingStoppedV3(
                  b.toBooking(Left(end.toLocalDateTimeWithZone))))(
                afterEventPersisted)
            }
          }
      }
    case RemoveBookingCommand(userReference,
                              organisationReference,
                              bookingId) =>
      log.debug(s"RemoveBooking, current state:$state")
      state match {
        case b: UserTimeBooking =>
          b.bookings
            .find(b =>
              b.id == bookingId && b.userReference == userReference && b.organisationReference == organisationReference)
            .foreach { removedB =>
              log.debug(s"RemoveBooking, found existing booking:$removedB")
              persist(UserTimeBookingRemovedV3(removedB))(afterEventPersisted)
            }
      }
    case UpdateBookingCommand(userReference,
                              organisationReference,
                              bookingId,
                              projectReference,
                              tags,
                              start,
                              endOrDuration) =>
      log.debug(s"EditBooking, current state:$state")
      state match {
        case b: UserTimeBooking =>
          b.bookingInProgress
            .find(b =>
              b.userReference == userReference && b.organisationReference == organisationReference && b.id == bookingId)
            .foreach { currentBooking =>
              val updatedStart = start
                .map(_.toLocalDateTimeWithZone)
                .getOrElse(currentBooking.start)
              val newBooking = currentBooking.copy(
                projectReference =
                  projectReference.orElse(currentBooking.projectReference),
                tags = tags.getOrElse(currentBooking.tags),
                start = updatedStart,
                // recalculate hash
                bookingHash = (projectReference, tags) match {
                  case (None, None) => currentBooking.bookingHash
                  case (_, _) =>
                    BookingHash.createHash(
                      projectReference.orElse(currentBooking.projectReference),
                      tags.getOrElse(currentBooking.tags))
                }
              )
              log.debug(
                s"EditBooking, found existing booking in progress :$currentBooking, updatedBooking: $newBooking")
              persist(
                UserTimeBookingInProgressEdited(currentBooking, newBooking))(
                afterEventPersisted)
            }

          b.bookings
            .find(b =>
              b.userReference == userReference && b.organisationReference == organisationReference && b.id == bookingId)
            .foreach { currentBooking =>
              val updatedStart = start
                .map(_.toLocalDateTimeWithZone)
                .getOrElse(currentBooking.start)
              val newBooking = currentBooking.copy(
                projectReference =
                  projectReference.orElse(currentBooking.projectReference),
                tags = tags.getOrElse(currentBooking.tags),
                start = updatedStart,
                end = endOrDuration
                  .map(
                    _.fold(end => Some(end.toLocalDateTimeWithZone),
                           // reset end in case an updated duration values was provided
                           _ => None))
                  .getOrElse(currentBooking.end),
                duration = endOrDuration
                  .map(
                    // recalculate duration in case an end was provided
                    _.fold(new org.joda.time.Duration(updatedStart.toDateTime,
                                                      _),
                           identity)
                  )
                  .getOrElse(
                    // recalculate as start might have changed
                    currentBooking.end.fold(currentBooking.duration)(
                      currentEnd =>
                        new org.joda.time.Duration(updatedStart.toDateTime,
                                                   currentEnd.toDateTime))
                  ),
                // recalculate hash
                bookingHash = (projectReference, tags) match {
                  case (None, None) => currentBooking.bookingHash
                  case (_, _) =>
                    BookingHash.createHash(
                      projectReference.orElse(currentBooking.projectReference),
                      tags.getOrElse(currentBooking.tags))
                }
              )
              log.debug(
                s"EditBooking, found existing booking:$currentBooking, updatedBooking: $newBooking")
              persist(UserTimeBookingEditedV4(currentBooking, newBooking))(
                afterEventPersisted)
            }
      }
    case AddBookingCommand(bookingType,
                           userReference,
                           organisationReference,
                           projectReference,
                           tags,
                           start,
                           endOrDuration) =>
      persist(
        UserTimeBookingAddedV3(
          id = newBookingId,
          bookingType = bookingType,
          start = start,
          endOrDuration = endOrDuration,
          userReference = userReference,
          organisationReference = organisationReference,
          projectReference = projectReference,
          tags = tags
        ))(afterEventPersisted)
    case ChangeStartTimeOfBooking(userReference,
                                  organisationReference,
                                  bookingId,
                                  newStart) =>
      state match {
        case b: UserTimeBooking =>
          b.bookingInProgress
            .find(b =>
              b.id == bookingId && b.userReference == userReference && b.organisationReference == organisationReference)
            .foreach { b =>
              val oldStart = b.start
              persist(
                UserTimeBookingStartTimeChanged(b.id,
                                                oldStart.toDateTime,
                                                newStart))(afterEventPersisted)
            }
      }
    case KillAggregate =>
      context.stop(self)
    case GetState =>
      sender() ! state
    // control commands
    case _: StartAggregate =>
      log.debug(s"StartAggregate for user: $userReference")
    case other =>
      log.warning(s"Received unknown command $other")
  }

  private def notifyClient(event: OutEvent): Unit = {
    if (!recovering) {
      clientReceiver ! (userReference.id, event, List(userReference.id))
    }
  }

  val removed: Receive = defaultReceive.orElse {
    case GetState =>
      log.warning(s"Received command in state removed")
      sender() ! state
    case KillAggregate =>
      log.warning(s"Received KillAggregate command in state removed")
      context.stop(self)
  }

  override val receiveCommand: Receive = uninitialized
}
