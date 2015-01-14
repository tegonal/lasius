package domain

import models._
import org.joda.time.DateTime
import akka.actor._
import akka.persistence._
import java.util.UUID

object UserBookingAggregate {
  import AggregateRoot._

  case class UserBookingStarted(projectId: ProjectId, tags: Seq[TagId], start: DateTime) extends Event
  case class UserBookingStopped(bookingId: BookingId, end: DateTime) extends Event
  case class UserBookingRemoved(bookingId: BookingId) extends Event
  case class UserBookingAdded(projectId: ProjectId, tags: Seq[TagId], start: DateTime, end: Option[DateTime]) extends Event

  case class UserBooking(userId: UserId, bookings: Seq[Booking]) extends State {
    def bookingInProgress = {
      bookings.filter(_.end.isEmpty).headOption
    }
  }

  case object KillAggregate extends Command
  trait UserBookingCommand extends Command {
    val userId: UserId
  }

  case class StartBooking(userId: UserId, projectId: ProjectId, tags: Seq[TagId], start: DateTime) extends UserBookingCommand
  case class EndBooking(userId: UserId, bookingId: BookingId, end: DateTime) extends UserBookingCommand

  def props(userId: UserId): Props = Props(new UserBookingAggregate(userId))

}

class UserBookingAggregate(userId: UserId) extends AggregateRoot {
  import UserBookingAggregate._
  import AggregateRoot._

  override def persistenceId: String = userId.value

  override var state: State = UserBooking(userId, Seq())

  /**
   * Updates internal processor state according to event that is to be applied.
   *
   * @param evt Event to apply
   */
  override def updateState(evt: Event): Unit = evt match {
    case UserBookingStarted(projectId, tags, start) =>
      log.debug(s"UserBookingStarted - projectId: $projectId, tags:$tags, start:$start")
      context.become(created)
      state = state match {
        case ub: UserBooking => startUserBooking(ub, projectId, tags, start, None)
        case _ => state
      }
    case UserBookingStopped(bookingId, end) =>
      log.debug(s"UserBookingStopped - bookingId: $bookingId, end: $end")
      state = state match {
        case ub: UserBooking => endUserBooking(ub, bookingId, end)
        case _ => state
      }
    case UserBookingRemoved(bookingId) =>
      log.debug(s"UserBookingRemoved - bookingId: $bookingId")
      state = state match {
        case ub: UserBooking => removeUserBooking(ub, bookingId)
        case _ => state
      }
    case UserBookingAdded(projectId, tags, start, end) =>
      log.debug(s"UserBookingAdded - project: $projectId, tags: $tags, time: $start - $end")
      context.become(created)
      state = state match {
        case ub: UserBooking => startUserBooking(ub, projectId, tags, start, end)
        case _ => state
      }
  }

  def startUserBooking(ub: UserBooking, projectId: ProjectId, tags: Seq[TagId], start: DateTime, end: Option[DateTime]) = {
    val newBooking = Booking(BookingId(UUID.randomUUID().toString()), start, end, userId, projectId, tags)
    ub.copy(bookings = ub.bookings :+ newBooking)
  }

  def endUserBooking(ub: UserBooking, bookingId: BookingId, end: DateTime) = {
    val newBookings = ub.bookings.map { b =>
      if (b.id == bookingId) b.copy(end = Some(end))
      else b
    }
    ub.copy(bookings = newBookings)
  }

  def removeUserBooking(ub: UserBooking, bookingId: BookingId) = {
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
    case StartBooking(userId, projectId, tags, start) =>
      //if another booking is still in progress
      state match {
        case b: UserBooking =>
          b.bookingInProgress.map(b => persist(UserBookingStopped(b.id, start))(afterEventPersisted))
      }

      persist(UserBookingStarted(projectId, tags, start))(afterEventPersisted)
    case EndBooking(userId, bookingId, end) =>
      //if another booking is still in progress
      persist(UserBookingStopped(bookingId, end))(afterEventPersisted)
    case KillAggregate =>
      context.stop(self)
  }

  val removed: Receive = {
    case KillAggregate =>
      context.stop(self)
  }
}