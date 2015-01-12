package domain

import models._
import org.joda.time.DateTime
import akka.actor._
import akka.persistence._

object UserBookingAggregate {

  trait State
  trait Command
  trait Event

  case class UserBooking(userId: UserId, bookings: Seq[Booking]) extends State

  trait UserBookingCommand extends Command {
    val userId: UserId
  }

  case class StartBooking(userId: UserId, projectId: ProjectId, tags: Seq[TagId], start: DateTime) extends UserBookingCommand

  def props(userId: UserId): Props = Props(new UserBookingAggregate(userId))

}

class UserBookingAggregate(userId: UserId) extends PersistentActor with ActorLogging {
  override def persistenceId: String = userId.value

  /**
   * Updates internal processor state according to event that is to be applied.
   *
   * @param evt Event to apply
   */
  def updateState(evt: UserBookingAggregate.Event): Unit = {
    ()
  }

  private def publish(event: UserBookingAggregate.Event) =
    context.system.eventStream.publish(event)

  val receiveCommand: Receive = {
    case _ =>
      log.debug("received command")
  }

  override val receiveRecover: Receive = {
    case evt: UserBookingAggregate.Event =>
      updateState(evt)
    case SnapshotOffer(metadata, state: UserBookingAggregate.State) =>
      restoreFromSnapshot(metadata, state)
      log.debug("recovering aggregate from snapshot")
  }

  protected def restoreFromSnapshot(metadata: SnapshotMetadata, state: UserBookingAggregate.State) = {

  }
}