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

package services

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import domain.AggregateRoot.{
  ForwardPersistentEvent,
  InitializeViewLive,
  RestoreViewFromState
}
import models.UserId.UserReference
import models.{EntityReference, UserId}

object UserService {
  case class StopUserView(userReference: UserReference)

  case class StartUserTimeBookingView(userReference: UserReference)
}

abstract class UserService[C] extends Actor with ActorLogging {

  import UserService._

  /** Implicit convertion from userid object model to string based
    * representation used in akka system
    */
  implicit def userId2String(userReference: UserReference): String =
    userReference.id.value.toString()

  protected def findOrCreate(id: UserReference): ActorRef =
    context.child(id).getOrElse(create(id))

  /** Processes aggregate command. oCreates an aggregate (if not already
    * created) and handles commands caching while aggregate is being killed.
    *
    * @param aggregateId
    *   Aggregate id
    * @param command
    *   Command that should be passed to aggregate
    */
  def processAggregateCommand(aggregateId: UserReference, command: C): Unit = {
    val maybeChild = context.child(aggregateId)
    log.debug(
      s"processAgregateCommand -> addregateId:$aggregateId, child:$maybeChild")
    maybeChild match {
      case Some(child) =>
        child.forward(command)
      case None =>
        val child = create(aggregateId)
        log.debug(s"forwardCommand to $child")
        child.forward(command)
    }
  }

  def processCommand: Receive

  def restoreViewFromState(restoreViewFromState: RestoreViewFromState): Unit = {
    log.debug(
      s"restoreViewFromSnapshot -> aggregateId:${restoreViewFromState.userReference.id.value}")
    findOrCreate(restoreViewFromState.userReference).forward(
      restoreViewFromState)
  }

  def initializeViewLive(initializeViewLive: InitializeViewLive): Unit = {
    log.debug(
      s"restoreViewFromSnapshot -> aggregateId:${initializeViewLive.userReference.id.value}")
    findOrCreate(initializeViewLive.userReference).forward(initializeViewLive)
  }

  def forwardPersistentEvent(
      forwardPersistentEvent: ForwardPersistentEvent): Unit = {
    log.debug(
      s"forwardPersistentEvent -> aggregateId: ${forwardPersistentEvent.userReference.id.value}")
    findOrCreate(forwardPersistentEvent.userReference)
      .forward(forwardPersistentEvent.event)
  }

  def removeUserView(userReference: UserReference): Unit = {
    val maybeChild = context.child(userReference)
    maybeChild match {
      case Some(child) =>
        context.stop(child)
      case _ =>
    }
  }

  override def receive: Receive = {
    case StopUserView(userReference) =>
      log.debug(s"StopUserView:${userReference.id.value}")
      removeUserView(userReference)
    case c =>
      log.debug(s"processCommand:$c")
      processCommand(c)
  }

  protected def create(id: UserReference): ActorRef = {
    val agg = context.actorOf(aggregateProps(id), id)
    context.watch(agg)
    agg
  }

  def aggregateProps(id: UserReference): Props
}
