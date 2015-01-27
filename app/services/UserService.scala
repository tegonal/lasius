package services

import akka.actor.ActorLogging
import akka.actor.Actor
import akka.actor.ActorRef
import domain.UserTimeBookingAggregate
import akka.actor.Terminated
import models.UserId
import models.ProjectId
import models.TagId
import org.joda.time.DateTime
import akka.actor.Props

abstract class UserService[C] extends Actor with ActorLogging {

  /**
   * Implicit convertion from userid object model to string based representation used in akka system
   */
  implicit def userId2String(userId: UserId): String = userId.value

  protected def findOrCreate(id: UserId): ActorRef =
    context.child(id) getOrElse create(id)

  /**
   * Processes aggregate command.
   * oCreates an aggregate (if not already created) and handles commands caching while aggregate is being killed.
   *
   * @param aggregateId Aggregate id
   * @param command Command that should be passed to aggregate
   */
  def processAggregateCommand(aggregateId: UserId, command: C) = {
    val maybeChild = context child aggregateId
    log.debug(s"processAgregateCommand -> addregateId:$aggregateId, child:$maybeChild")
    maybeChild match {
      case Some(child) =>
        child forward command
      case None =>
        val child = create(aggregateId)
        log.debug(s"forwardCommand to $child")
        child forward command
    }
  }

  def processCommand: Receive

  override def receive = processCommand

  
  protected def create(id: UserId): ActorRef = {
    val agg = context.actorOf(aggregateProps(id), id)
    context watch agg
    agg
  }

  def aggregateProps(id: UserId):Props
}