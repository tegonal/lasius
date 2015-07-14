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
package domain

import models._
import org.joda.time.DateTime
import akka.actor._
import akka.persistence._
import java.util.UUID
import play.api.Logger
import repositories.UserBookingHistoryRepositoryComponent
import actors.ClientReceiverComponent
import scala.concurrent.ExecutionContext.Implicits.global
import actors.DefaultClientReceiverComponent
import repositories.MongoUserBookingHistoryRepositoryComponent

object UserTimeBookingAggregate {
  import AggregateRoot._

  case class UserTimeBookingInitialized(userId: UserId) extends Event
  case class UserTimeBookingStarted(booking: Booking) extends Event
  case class UserTimeBookingStopped(booking: Booking) extends Event
  case class UserTimeBookingPaused(bookingId: BookingId, time: DateTime) extends Event
  case class UserTimeBookingRemoved(booking: Booking) extends Event
  case class UserTimeBookingAdded(booking: Booking) extends Event
  case class UserTimeBookingEdited(booking: Booking) extends Event
  case class UserTimeBookingStartTimeChanged(bookingId: BookingId, fromStart: DateTime, toStart: DateTime) extends Event

  case class UserTimeBooking(userId: UserId, bookings: Seq[Booking]) extends State {
    def bookingInProgress = {
      bookings.filter(_.end.isEmpty).headOption
    }
  }

  case object KillAggregate extends Command
  trait UserTimeBookingCommand extends Command {
    val userId: UserId
  }

  case class StartAggregate(userId: UserId) extends UserTimeBookingCommand
  case class ChangeStartTimeOfBooking(userId: UserId, bookingId: BookingId, newStart: DateTime) extends UserTimeBookingCommand
  case class StartBooking(userId: UserId, categoryId: CategoryId, projectId: ProjectId, tags: Seq[TagId], start: DateTime) extends UserTimeBookingCommand
  case class EndBooking(userId: UserId, bookingId: BookingId, end: DateTime) extends UserTimeBookingCommand
  case class PauseBooking(userId: UserId, bookingId: BookingId, date: DateTime) extends UserTimeBookingCommand
  case class ResumeBooking(userId: UserId, bookingId: BookingId, date: DateTime) extends UserTimeBookingCommand
  case class RemoveBooking(userId: UserId, bookingId: BookingId) extends UserTimeBookingCommand
  case class AppendBooking(userId: UserId, categoryId: CategoryId, projectId: ProjectId, tags: Seq[TagId], start: DateTime, end: DateTime) extends UserTimeBookingCommand
  case class EditBooking(userId: UserId, bookingId: BookingId, start: DateTime, end: DateTime) extends UserTimeBookingCommand

  def props(userId: UserId): Props = Props(classOf[MongoUserTimeBookingAggregate], userId)

}

class MongoUserTimeBookingAggregate(userId: UserId) extends UserTimeBookingAggregate(userId)
  with MongoUserBookingHistoryRepositoryComponent with DefaultClientReceiverComponent

class UserTimeBookingAggregate(userId: UserId) extends AggregateRoot {
  this: UserBookingHistoryRepositoryComponent with ClientReceiverComponent =>
  import UserTimeBookingAggregate._
  import AggregateRoot._

  log.debug(s"UserTimeBookingAggregate: created $userId")

  override def persistenceId: String = userId.value

  override var state: State = UserTimeBooking(userId, Seq())

  def newBookingId = BookingId(UUID.randomUUID().toString())

  /**
   * Updates internal processor state according to event that is to be applied.
   *
   * @param evt Event to apply
   */
  override def updateState(evt: Event): Unit = {
    log.debug(s"updateStart:$evt")
    evt match {
      case e: UserTimeBookingInitialized =>
        log.debug(s"UserTimeBookingInitialized")
        context become created

        bookingHistoryRepository.deleteByUser(userId)
        notifyClient(UserTimeBookingHistoryEntryCleaned(userId))
      case UserTimeBookingStarted(booking) =>
        log.debug(s"UserBookingStarted - $booking")
        state = state match {
          case ub: UserTimeBooking => startUserBooking(ub, booking)
          case _ => state
        }
      case UserTimeBookingStopped(booking) =>
        log.debug(s"UserBookingStopped - $booking")
        state = state match {
          case ub: UserTimeBooking => endAndLogUserBooking(ub, booking)
          case _ => state
        }
      case UserTimeBookingPaused(bookingId, time) =>
        log.debug(s"UserBookingPaused - $bookingId")
        state = state match {
          case ub: UserTimeBooking => endUserBooking(ub, bookingId, time)
          case _ => state
        }
      case UserTimeBookingRemoved(booking) =>
        log.debug(s"UserBookingRemoved - $booking")
        state = state match {
          case ub: UserTimeBooking => removeUserBooking(ub, booking)
          case _ => state
        }
      case UserTimeBookingAdded(booking) =>
        log.debug(s"UserBookingAdded - $booking")
        state = state match {
          case ub: UserTimeBooking => startUserBooking(ub, booking)
          case _ => state
        }
      case UserTimeBookingEdited(booking) =>
        log.debug(s"UserBookingEdited- $booking")
        state = state match {
          case ub: UserTimeBooking => editUserBooking(ub, booking)
          case _ => state
        }
      case UserTimeBookingStartTimeChanged(bookingId, fromStart, toStart) =>
        log.debug(s"UserBookingStartTimeChanged - $bookingId - $fromStart -> $toStart")
        state = state match {
          case ub: UserTimeBooking => updateStartTime(ub, bookingId, fromStart, toStart)
          case _ => state
        }
    }
  }

  def startUserBooking(ub: UserTimeBooking, booking: Booking) = {
    if (booking.end.isDefined) {
      bookingHistoryRepository.insert(booking)
      notifyClient(UserTimeBookingHistoryEntryAdded(booking))
    }

    ub.copy(bookings = ub.bookings :+ booking)
  }

  def endAndLogUserBooking(ub: UserTimeBooking, booking: Booking) = {
    bookingHistoryRepository.insert(booking)
    notifyClient(UserTimeBookingHistoryEntryAdded(booking))
    endUserBooking(ub, booking.id, booking.end.get)
  }

  def endUserBooking(ub: UserTimeBooking, bookingId: BookingId, endTime: DateTime) = {
    val newBookings = ub.bookings.map { b =>
      if (b.id == bookingId) b.copy(end = Some(endTime))
      else b
    }
    ub.copy(bookings = newBookings)
  }

  def removeUserBooking(ub: UserTimeBooking, booking: Booking) = {
    bookingHistoryRepository.coll.remove(booking)
    notifyClient(UserTimeBookingHistoryEntryRemoved(booking.id))

    val newBookings = ub.bookings.filter(_.id != booking.id)
    ub.copy(bookings = newBookings)
  }

  def editUserBooking(ub: UserTimeBooking, booking: Booking) = {
    val newBookings = ub.bookings.map { b =>
      if (b.id == booking.id) b.copy(start = booking.start, end = booking.end)
      else b
    }
    ub.copy(bookings = newBookings)
  }

  def updateStartTime(ub: UserTimeBooking, bookingId: BookingId, fromStart: DateTime, toStart: DateTime) = {
    val newBookings = ub.bookings.map { b =>
      if (b.id == bookingId) b.copy(start = toStart)
      else b
    }
    ub.copy(bookings = newBookings)
  }

  override def restoreFromSnapshot(metadata: SnapshotMetadata, state: State) = {
    state match {
      case Removed => context become removed
      case Created => context become created
      case _: User => context become uninitialized
      case s: UserTimeBooking => this.state = s
    }
  }

  val uninitialized: Receive = {

    case GetState =>
      sender ! state
    case Initialize(state) =>
      log.debug(s"Initialize: $state")
      this.state = state
      context become created
    case e =>
      log.debug(s"InitBooking -> userId: $userId:$e")
      persist(UserTimeBookingInitialized(userId))(afterEventPersisted)
      context become created
      created(e)
  }

  val created: Receive = {
    case StartBooking(_, categoryId, projectId, tags, start) =>
      log.debug(s"StartBooking -> projectId:$projectId, tags:$tags, start:$start")
      //if another booking is still in progress
      state match {
        case b: UserTimeBooking => stopBookingInProgress(b, start)
      }

      val newBooking = Booking(newBookingId, start, None, userId, categoryId, projectId, tags)
      persist(UserTimeBookingStarted(newBooking))(afterEventPersisted)
    case EndBooking(_, bookingId, end) =>
      log.debug(s"EndBooking -> bookingId:$bookingId")
      state match {
        case b: UserTimeBooking =>
          b.bookingInProgress.map { b =>
            if (b.id == bookingId) {
              val stoppedB = b.copy(end = Some(end))
              persist(UserTimeBookingStopped(stoppedB))(afterEventPersisted)
            }
          }
      }
    case RemoveBooking(_, bookingId) =>
      log.debug(s"RemoveBooking, current state:$state")
      state match {
        case b: UserTimeBooking =>
          b.bookings.find(_.id == bookingId) map { removedB =>
            log.debug(s"RemoveBooking, found existing booking:$removedB")
            persist(UserTimeBookingRemoved(removedB))(afterEventPersisted)
          }
      }
    case EditBooking(_, bookingId, start, end) =>
      log.debug(s"EditBooking, current state:$state")
      state match {
        case b: UserTimeBooking =>
          b.bookings.find(_.id == bookingId) map { edited =>
            log.debug(s"EditBooking, found existing booking:$edited")
            persist(UserTimeBookingEdited(edited))(afterEventPersisted)
          }
      }
    case AppendBooking(userId, categoryId, projectId, tags, start, end) =>
      persist(UserTimeBookingAdded(Booking(newBookingId, start, Some(end), userId, categoryId, projectId, tags)))(afterEventPersisted)
    case PauseBooking(userId, bookingId, time) =>
      state match {
        case b: UserTimeBooking =>
          b.bookings.find(_.id == bookingId) map { b =>
            val pausedB = b.copy(end = Some(time))
            log.debug(s"PauseBooking, found existing booking:$pausedB")
            persist(UserTimeBookingPaused(pausedB.id, time))(afterEventPersisted)
          }
      }
    case ResumeBooking(userId, bookingId, time) =>
      state match {
        case b: UserTimeBooking =>
          b.bookings.find(b => b.id == bookingId && b.end.isDefined).map { pausedB =>
            log.debug(s"ResumeBooking, found existing booking:$bookingId")
            //first stop booking in progress
            stopBookingInProgress(b, time)

            val resumedB = Booking(newBookingId, time, None, userId, pausedB.categoryId, pausedB.projectId, pausedB.tags)
            log.debug(s"ResumedBooking, found existing booking:$resumedB")
            persist(UserTimeBookingStarted(resumedB))(afterEventPersisted)
          }.getOrElse {
            log.debug(s"ResumeBooking: didn't find paused booking with id:$bookingId")
          }
      }
    case ChangeStartTimeOfBooking(userId, bookingId, newStart) =>
      state match {
        case b: UserTimeBooking =>
          b.bookings.find(_.id == bookingId).filter(!_.end.isDefined) map { b =>
            val oldStart = b.start
            persist(UserTimeBookingStartTimeChanged(b.id, oldStart, newStart))(afterEventPersisted)
          }
      }
    case KillAggregate =>
      context.stop(self)
    case GetState =>
      sender ! state
    case other =>
      log.debug(s"Received unknown command")
  }

  def stopBookingInProgress(b: UserTimeBooking, time: DateTime) = {
    b.bookingInProgress.map { b =>
      val stoppedB = b.copy(end = Some(time))
      persist(UserTimeBookingStopped(stoppedB))(afterEventPersisted)
    }
  }

  private def notifyClient(event: OutEvent) = {
    clientReceiver ! (userId, event, List(userId))
  }

  val removed: Receive = {
    case GetState =>
      sender() ! state
    case KillAggregate =>
      context.stop(self)
  }

  override val receiveCommand: Receive = uninitialized
}