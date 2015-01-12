package services

import akka.actor.ActorLogging
import akka.actor.Actor
import akka.actor.ActorRef
import domain.UserBookingAggregate
import akka.actor.Terminated
import models.UserId
import models.ProjectId
import models.TagId
import org.joda.time.DateTime
import akka.actor.Props
import domain.UserBookingAggregate.UserBookingCommand

object BookingManagerService {

  def props: Props = Props(new BookingManagerService)
}

class BookingManagerService extends Actor with ActorLogging {

  /**
   * Implicit convertion from userid object model to string based representation used in akka system
   */
  implicit def userId2String(userId: UserId): String = userId.value

  protected def findOrCreate(id: UserId): ActorRef =
    context.child(id) getOrElse create(id)

  /**
   * Processes aggregate command.
   * Creates an aggregate (if not already created) and handles commands caching while aggregate is being killed.
   *
   * @param aggregateId Aggregate id
   * @param command Command that should be passed to aggregate
   */
  def processAggregateCommand(aggregateId: UserId, command: UserBookingCommand) = {
    val maybeChild = context child aggregateId
    maybeChild match {
      case Some(child) =>
        child forward command
      case None =>
        val child = create(aggregateId)
        child forward command
    }
  }

  def processCommand: Receive = {
    case cmd: UserBookingCommand =>
      processAggregateCommand(cmd.userId, cmd)
  }

  override def receive = processCommand

  protected def create(id: UserId): ActorRef = {
    val agg = context.actorOf(aggregateProps(id), id)
    context watch agg
    agg
  }

  def aggregateProps(id: UserId) = UserBookingAggregate.props(id)
}