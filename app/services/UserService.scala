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
package services

import akka.actor.ActorLogging
import akka.actor.Actor
import akka.actor.ActorRef
import domain.UserTimeBookingAggregate
import akka.actor.Terminated
import models.UserId
import models.TagId
import org.joda.time.DateTime
import akka.actor.Props

object UserService {
  case class StopUserView(userId: UserId)

  case class StartUserTimeBookingView(userId: UserId)
}

abstract class UserService[C] extends Actor with ActorLogging {
  import UserService._

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

  def removeUserView(userId: UserId) = {
    val maybeChild = context child userId
    maybeChild match {
      case Some(child) =>
        context stop child
      case _ =>
    }
  }

  override def receive = {
    case StopUserView(userId) =>
      log.debug(s"StopUserView:$userId")
      removeUserView(userId)
    case c =>
      log.debug(s"processCommand:$c")
      processCommand(c)
  }

  protected def create(id: UserId): ActorRef = {
    val agg = context.actorOf(aggregateProps(id), id)
    context watch agg
    agg
  }

  def aggregateProps(id: UserId): Props
}