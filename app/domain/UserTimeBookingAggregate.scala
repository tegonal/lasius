package domain

import models._
import org.joda.time.DateTime
import akka.actor._
import akka.persistence._
import java.util.UUID
import play.api.Logger

object UserTimeBookingAggregate {
  import AggregateRoot._

  case class UserTimeBookingInitialized(userId: UserId) extends Event
  case class UserTimeBookingStarted(booking: Booking) extends Event
  case class UserTimeBookingStopped(booking: Booking) extends Event
  case class UserTimeBookingRemoved(booking: Booking) extends Event
  case class UserTimeBookingAdded(booking: Booking) extends Event
  case class UserTimeBookingEdited(booking: Booking) extends Event

  case class UserTimeBooking(userId: UserId, bookings: Seq[Booking]) extends State {
    def bookingInProgress = {
      bookings.filter(_.end.isEmpty).headOption
    }
  }

  case object KillAggregate extends Command
  trait UserTimeBookingCommand extends Command {
    val userId: UserId
  }

  case class StartBooking(userId: UserId, categoryId: CategoryId, projectId: ProjectId, tags: Seq[TagId], start: DateTime) extends UserTimeBookingCommand
  case class EndBooking(userId: UserId, bookingId: BookingId, end: DateTime) extends UserTimeBookingCommand
  case class RemoveBooking(userId: UserId, bookingId: BookingId) extends UserTimeBookingCommand
  case class AppendBooking(userId: UserId, categoryId: CategoryId, projectId: ProjectId, tags: Seq[TagId], start: DateTime, end: DateTime) extends UserTimeBookingCommand
  case class EditBooking(userId: UserId, bookingId: BookingId, start: DateTime, end: DateTime) extends UserTimeBookingCommand

  def props(userId: UserId): Props = {
    Logger.debug(s"Create actor:$userId")
    Props(new UserTimeBookingAggregate(userId))
  }

}

class UserTimeBookingAggregate(userId: UserId) extends AggregateRoot {
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
      case UserTimeBookingStarted(booking) =>
        log.debug(s"UserBookingStarted - $booking")
        state = state match {
          case ub: UserTimeBooking => startUserBooking(ub, booking)
          case _ => state
        }
      case UserTimeBookingStopped(booking) =>
        log.debug(s"UserBookingStopped - $booking")
        state = state match {
          case ub: UserTimeBooking => endUserBooking(ub, booking)
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
    }
  }

  def startUserBooking(ub: UserTimeBooking, booking: Booking) = {
    ub.copy(bookings = ub.bookings :+ booking)
  }

  def endUserBooking(ub: UserTimeBooking, booking: Booking) = {
    val newBookings = ub.bookings.map { b =>
      if (b.id == booking.id) b.copy(end = booking.end)
      else b
    }
    ub.copy(bookings = newBookings)
  }

  def removeUserBooking(ub: UserTimeBooking, booking: Booking) = {
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
        case b: UserTimeBooking =>
          b.bookingInProgress.map { b =>
            val stoppedB = b.copy(end = Some(start))
            persist(UserTimeBookingStopped(stoppedB))(afterEventPersisted)
          }
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
    case KillAggregate =>
      context.stop(self)
    case GetState =>
      sender ! state
    case other =>
      log.debug(s"Received unknown command")
  }

  val removed: Receive = {
    case GetState =>
      sender() ! state
    case KillAggregate =>
      context.stop(self)
  }

  override val receiveCommand: Receive = uninitialized
}