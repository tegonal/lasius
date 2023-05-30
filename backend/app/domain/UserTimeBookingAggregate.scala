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

  case class UserTimeBooking(userReference: UserReference,
                             bookings: Seq[BookingV2])
      extends State {
    def bookingInProgress: Option[BookingV2] = {
      bookings.find(_.end.isEmpty)
    }
  }

  object UserTimeBooking {
    implicit val userTimeBookingFormat: Format[UserTimeBooking] =
      Json.using[Json.WithDefaultValues].format[UserTimeBooking]
  }

  case object KillAggregate extends Command

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

  case class StartBookingCommand(userReference: UserReference,
                                 organisationReference: OrganisationReference,
                                 projectReference: ProjectReference,
                                 tags: Set[Tag],
                                 start: DateTime)
      extends UserTimeBookingCommand

  case class EndBookingCommand(userReference: UserReference,
                               organisationReference: OrganisationReference,
                               bookingId: BookingId,
                               end: DateTime)
      extends UserTimeBookingCommand

  case class RemoveBookingCommand(userReference: UserReference,
                                  organisationReference: OrganisationReference,
                                  bookingId: BookingId)
      extends UserTimeBookingCommand

  case class AddBookingCommand(userReference: UserReference,
                               organisationReference: OrganisationReference,
                               projectReference: ProjectReference,
                               tags: Set[Tag],
                               start: DateTime,
                               end: DateTime)
      extends UserTimeBookingCommand

  case class EditBookingCommand(userReference: UserReference,
                                organisationReference: OrganisationReference,
                                bookingId: BookingId,
                                projectReference: Option[ProjectReference],
                                tags: Option[Set[Tag]],
                                start: Option[DateTime],
                                end: Option[Option[DateTime]])
      extends UserTimeBookingCommand

  def props(systemServices: SystemServices,
            clientReceiver: ClientReceiver,
            bookingHistoryRepository: BookingHistoryRepository,
            userReference: UserReference,
            reactiveMongoApi: ReactiveMongoApi): Props =
    Props(classOf[UserTimeBookingAggregate],
          systemServices,
          clientReceiver,
          bookingHistoryRepository,
          userReference,
          reactiveMongoApi)

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

  override var state: State = UserTimeBooking(userReference, Seq())

  implicit val executionContext: ExecutionContextExecutor = context.dispatcher

  def newBookingId: BookingId = BookingId()

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
      case UserTimeBookingStartedV2(booking) =>
        log.debug(s"UserBookingStarted - $booking")
        state = state match {
          case ub: UserTimeBooking => startUserBooking(ub, booking)
          case _                   => state
        }
      case UserTimeBookingStoppedV2(booking) =>
        log.debug(s"UserBookingStopped - $booking")
        state = state match {
          case ub: UserTimeBooking => endAndLogUserBooking(ub, booking)
          case _                   => state
        }
      case e: UserTimeBookingPaused @nowarn("cat=deprecation") =>
        log.debug(s"UserBookingPaused - ${e.bookingId}")
        state = state match {
          case ub: UserTimeBooking =>
            endUserBooking(ub, e.bookingId, e.time.toLocalDateTimeWithZone())
          case _ => state
        }
      case UserTimeBookingRemovedV2(booking) =>
        log.debug(s"UserBookingRemoved - $booking")
        state = state match {
          case ub: UserTimeBooking => removeUserBooking(ub, booking)
          case _                   => state
        }
      case UserTimeBookingAddedV2(booking) =>
        log.debug(s"UserBookingAdded - $booking")
        state = state match {
          case ub: UserTimeBooking => startUserBooking(ub, booking)
          case _                   => state
        }
      case UserTimeBookingEditedV3(booking, newBooking) =>
        log.debug(s"UserBookingEdited- $booking: $newBooking")
        state = state match {
          case ub: UserTimeBooking => editUserBooking(ub, newBooking)
          case _                   => state
        }
      case UserTimeBookingStartTimeChanged(bookingId, fromStart, toStart) =>
        log.debug(
          s"UserBookingStartTimeChanged - $bookingId - $fromStart -> $toStart")
        state = state match {
          case ub: UserTimeBooking =>
            updateStartTime(ub, bookingId, fromStart, toStart)
          case _ => state
        }
      case _ =>
    }
  }

  def startUserBooking(ub: UserTimeBooking,
                       booking: BookingV2): UserTimeBooking = {
    if (booking.end.isDefined) {
      notifyClient(UserTimeBookingHistoryEntryAdded(booking))
      Await.ready(withDBSession()(implicit dbSession =>
                    bookingHistoryRepository.upsert(booking)),
                  systemServices.duration)
    }

    ub.copy(bookings = ub.bookings :+ booking)
  }

  def endAndLogUserBooking(ub: UserTimeBooking,
                           booking: BookingV2): UserTimeBooking = {
    notifyClient(UserTimeBookingHistoryEntryAdded(booking))
    Await.ready(withDBSession()(implicit dbSession =>
                  bookingHistoryRepository.upsert(booking)),
                systemServices.duration)
    endUserBooking(ub, booking.id, booking.end.get)
  }

  def endUserBooking(ub: UserTimeBooking,
                     bookingId: BookingId,
                     endTime: LocalDateTimeWithTimeZone): UserTimeBooking = {
    val newBookings = ub.bookings.map { b =>
      if (b.id == bookingId)
        b.copy(end = Some(endTime))
      else b
    }
    ub.copy(bookings = newBookings)
  }

  def removeUserBooking(ub: UserTimeBooking,
                        booking: BookingV2): UserTimeBooking = {
    notifyClient(UserTimeBookingHistoryEntryRemoved(booking.id))
    Await.ready(withDBSession()(implicit dbSession =>
                  bookingHistoryRepository.remove(booking)),
                systemServices.duration)

    val newBookings = ub.bookings.filter(_.id != booking.id)
    ub.copy(bookings = newBookings)
  }

  def editUserBooking(ub: UserTimeBooking,
                      newBooking: BookingV2): UserTimeBooking = {
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

  def updateStartTime(ub: UserTimeBooking,
                      bookingId: BookingId,
                      fromStart: DateTime,
                      toStart: DateTime): UserTimeBooking = {
    val newBookings = ub.bookings.map { b =>
      if (b.id == bookingId)
        b.copy(start = toStart.toLocalDateTimeWithZone())
      else b
    }
    ub.copy(bookings = newBookings)
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
              _ <- bookingHistoryRepository.bulkInsert(
                s.bookings.filter(_.end.isDefined).toList)
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
    systemServices.latestUserTimeBookingsViewService ! message
    systemServices.timeBookingStatisticsViewService ! message
    systemServices.currentOrganisationTimeBookingsView ! message
  }

  val uninitialized: Receive = defaultReceive.orElse {
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

  val created: Receive = defaultReceive.orElse {
    case StartBookingCommand(_,
                             organisationReference,
                             projectReference,
                             tags,
                             start) =>
      log.debug(
        s"StartBooking -> projectId:${projectReference.id}, tags:$tags, start:$start")
      // if another booking is still in progress
      state match {
        case b: UserTimeBooking => stopBookingInProgress(b, start)
      }

      val newBooking =
        BookingV2(newBookingId,
                  start.toLocalDateTimeWithZone(),
                  None,
                  userReference,
                  organisationReference,
                  projectReference,
                  tags)
      persist(UserTimeBookingStartedV2(newBooking))(afterEventPersisted)
    case EndBookingCommand(userReference,
                           organisationReference,
                           bookingId,
                           end) =>
      log.debug(s"EndBooking -> bookingId:$bookingId")
      state match {
        case b: UserTimeBooking =>
          b.bookingInProgress.foreach { b =>
            if (b.id == bookingId && b.userReference == userReference && b.organisationReference == organisationReference) {
              val stoppedB =
                b.copy(end = Some(end.toLocalDateTimeWithZone()))
              persist(UserTimeBookingStoppedV2(stoppedB))(afterEventPersisted)
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
              persist(UserTimeBookingRemovedV2(removedB))(afterEventPersisted)
            }
      }
    case EditBookingCommand(userReference,
                            organisationReference,
                            bookingId,
                            projectReference,
                            tags,
                            start,
                            end) =>
      log.debug(s"EditBooking, current state:$state")
      state match {
        case b: UserTimeBooking =>
          b.bookings
            .find(b =>
              b.userReference == userReference && b.organisationReference == organisationReference && b.id == bookingId)
            .foreach { edited =>
              val newBooking = edited.copy(
                projectReference =
                  projectReference.getOrElse(edited.projectReference),
                tags = tags.getOrElse(edited.tags),
                start = start
                  .map(_.toLocalDateTimeWithZone())
                  .getOrElse(edited.start),
                end = end
                  .map(_.map(_.toLocalDateTimeWithZone()))
                  .getOrElse(edited.end),
                // recalculate hash
                bookingHash = (projectReference, tags) match {
                  case (None, None) => edited.bookingHash
                  case (_, _) =>
                    BookingHash.createHash(
                      projectReference.getOrElse(edited.projectReference),
                      tags.getOrElse(edited.tags))
                }
              )
              log.debug(
                s"EditBooking, found existing booking:$edited, updatedBooking: $newBooking")
              persist(UserTimeBookingEditedV3(edited, newBooking))(
                afterEventPersisted)
            }
      }
    case AddBookingCommand(userReference,
                           organisationReference,
                           projectReference,
                           tags,
                           start,
                           end) =>
      persist(
        UserTimeBookingAddedV2(
          BookingV2(newBookingId,
                    start.toLocalDateTimeWithZone(),
                    Some(end.toLocalDateTimeWithZone()),
                    userReference,
                    organisationReference,
                    projectReference,
                    tags)))(afterEventPersisted)
    case ChangeStartTimeOfBooking(userReference,
                                  organisationReference,
                                  bookingId,
                                  newStart) =>
      state match {
        case b: UserTimeBooking =>
          b.bookings
            .find(b =>
              b.id == bookingId && b.userReference == userReference && b.organisationReference == organisationReference)
            .filter(_.end.isEmpty)
            .foreach { b =>
              val oldStart = b.start
              persist(
                UserTimeBookingStartTimeChanged(b.id,
                                                oldStart.toDateTime(),
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

  def stopBookingInProgress(b: UserTimeBooking, time: DateTime): Unit = {
    b.bookingInProgress.foreach { b =>
      val stoppedB = b.copy(end = Some(time.toLocalDateTimeWithZone()))
      persist(UserTimeBookingStoppedV2(stoppedB))(afterEventPersisted)
    }
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
