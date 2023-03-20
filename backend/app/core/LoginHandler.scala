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

package core

import akka.actor._
import akka.event.EventStream
import models.UserId.UserReference
import models._

object LoginHandler {

  def subscribe(ref: ActorRef, eventStream: EventStream) = {
    eventStream.subscribe(ref, classOf[UserLoggedInV2])
    eventStream.subscribe(ref, classOf[UserLoggedOutV2])
  }

  case class InitializeUserViews(userReference: UserReference)

  def props(systemServices: SystemServices): Props =
    Props(classOf[LoginHandler], systemServices)
}

class LoginHandler(systemServices: SystemServices)
    extends Actor
    with ActorLogging {

  import LoginHandler._
  import domain.UserTimeBookingAggregate._

  val receive: Receive = {
    case InitializeUserViews(userReference) =>
      initializeUserViews(userReference)
    case UserLoggedInV2(userReference) =>
      initializeUserViews(userReference)
    case UserLoggedOutV2(userReference) =>
      handleLoggedOut(userReference)
  }

  def initializeUserViews(userReference: UserReference) = {
    log.debug(s"user logged in:${userReference}, start persistentViews")
    // initialize persistentviews
    systemServices.timeBookingViewService ! StartAggregate(userReference)
  }

  def handleLoggedOut(userReference: UserReference) = {
    log.debug(s"user logged in:${userReference}, stop persistentViews")
  }
}
