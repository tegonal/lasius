package domain

import models._
import org.joda.time.DateTime
import akka.actor._
import akka.persistence._
import java.util.UUID

object UserTimeBookingAggregate {
  import AggregateRoot._

  case class UserTimeBookingStarted(projectId: ProjectId, tags: Seq[TagId], start: DateTime) extends Event
  case class UserTimeBookingStopped(bookingId: BookingId, end: DateTime) extends Event
  case class UserTimeBookingRemoved(bookingId: BookingId) extends Event
  case class UserTimeBookingAdded(projectId: ProjectId, tags: Seq[TagId], start: DateTime, end: DateTime) extends Event

  case class UserTimeBooking(userId: UserId, bookings: Seq[Booking]) extends State {
    def bookingInProgress = {
      bookings.filter(_.end.isEmpty).headOption
    }
  }

  case object KillAggregate extends Command
  trait UserTimeBookingCommand extends Command {
    val userId: UserId
  }

  case class StartBooking(userId: UserId, projectId: ProjectId, tags: Seq[TagId], start: DateTime) extends UserTimeBookingCommand
  case class EndBooking(userId: UserId, bookingId: BookingId, end: DateTime) extends UserTimeBookingCommand
  case class RemoveBooking(userId: UserId, bookingId: BookingId) extends UserTimeBookingCommand
  case class AppendBooking(userId: UserId, projectId: ProjectId, tags: Seq[TagId], start: DateTime, end: DateTime) extends UserTimeBookingCommand

  def props(userId: UserId): Props = Props(new UserTimeBookingAggregate(userId))

}

class UserTimeBookingAggregate(userId: UserId) extends AggregateRoot {
  import UserTimeBookingAggregate._
  import AggregateRoot._

  override def persistenceId: String = userId.value

  override var state: State = UserTimeBooking(userId, Seq())

  /**
   * Updates internal processor state according to event that is to be applied.
   *
   * @param evt Event to apply
   */
  override def updateState(evt: Event): Unit = evt match {
    case UserTimeBookingStarted(projectId, tags, start) =>
      log.debug(s"UserBookingStarted - projectId: $projectId, tags:$tags, start:$start")
      context.become(created)
      state = state match {
        case ub: UserTimeBooking => startUserBooking(ub, projectId, tags, start, None)
        case _ => state
      }
    case UserTimeBookingStopped(bookingId, end) =>
      log.debug(s"UserBookingStopped - bookingId: $bookingId, end: $end")
      state = state match {
        case ub: UserTimeBooking => endUserBooking(ub, bookingId, end)
        case _ => state
      }
    case UserTimeBookingRemoved(bookingId) =>
      log.debug(s"UserBookingRemoved - bookingId: $bookingId")
      state = state match {
        case ub: UserTimeBooking => removeUserBooking(ub, bookingId)
        case _ => state
      }
    case UserTimeBookingAdded(projectId, tags, start, end) =>
      log.debug(s"UserBookingAdded - project: $projectId, tags: $tags, time: $start - $end")
      context.become(created)
      state = state match {
        case ub: UserTimeBooking => startUserBooking(ub, projectId, tags, start, Some(end))
        case _ => state
      }
  }

  def startUserBooking(ub: UserTimeBooking, projectId: ProjectId, tags: Seq[TagId], start: DateTime, end: Option[DateTime]) = {
    val newBooking = Booking(BookingId(UUID.randomUUID().toString()), start, end, userId, projectId, tags)
    ub.copy(bookings = ub.bookings :+ newBooking)
  }

  def endUserBooking(ub: UserTimeBooking, bookingId: BookingId, end: DateTime) = {
    val newBookings = ub.bookings.map { b =>
      if (b.id == bookingId) b.copy(end = Some(end))
      else b
    }
    ub.copy(bookings = newBookings)
  }

  def removeUserBooking(ub: UserTimeBooking, bookingId: BookingId) = {
    val newBookings = ub.bookings.filter(_.id != bookingId)
    ub.copy(bookings = newBookings)
  }

  override def restoreFromSnapshot(metadata: SnapshotMetadata, state: State) = {
    this.state = state
    state match {
      case Removed => context become removed
      case _: User => context become created
    }
  }

  val created: Receive = {
    case StartBooking(_, projectId, tags, start) =>
      //if another booking is still in progress
      state match {
        case b: UserTimeBooking =>
          b.bookingInProgress.map(b => persist(UserTimeBookingStopped(b.id, start))(afterEventPersisted))
      }

      persist(UserTimeBookingStarted(projectId, tags, start))(afterEventPersisted)
    case EndBooking(_, bookingId, end) =>
      persist(UserTimeBookingStopped(bookingId, end))(afterEventPersisted)
    case RemoveBooking(_, bookingId) =>
      persist(UserTimeBookingRemoved(bookingId))(afterEventPersisted)
    case AppendBooking(_, bookingId, tags, start, end) =>
      persist(UserTimeBookingAdded(bookingId, tags, start, end))(afterEventPersisted)
    case KillAggregate =>
      context.stop(self)
  }

  val removed: Receive = {
    case KillAggregate =>
      context.stop(self)
  }
}