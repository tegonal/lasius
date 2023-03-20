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

package domain

import akka.actor._
import akka.event.LoggingReceive
import akka.pattern.StatusReply.Ack
import akka.persistence._
import domain.UserTimeBookingAggregate.StartAggregate
import domain.views.RestoreViewFromStateSuccess
import models.UserId.UserReference
import models._
import play.api.libs.json._

object LoginStateAggregate {

  import AggregateRoot._

  case class LoggedInState(loggedInUsers: Set[UserReference]) extends State

  object LoggedInState {
    implicit val loggedInStateFormat: Format[LoggedInState] =
      Json.using[Json.WithDefaultValues].format[LoggedInState]
  }

  def props: Props = Props(classOf[LoginStateAggregate])

  val persistenceId: String = "login-state"
}

class LoginStateAggregate extends AggregateRoot {

  import AggregateRoot._
  import LoginStateAggregate._

  override def persistenceId: String = LoginStateAggregate.persistenceId

  override var state: State = LoggedInState(Set())

  override def updateState(evt: PersistedEvent): Unit = {
    evt match {
      case UserLoggedInV2(userReference) =>
        state = state match {
          case ub: LoggedInState => userLoggerIn(ub, userReference)
          case _                 => state
        }
      case UserLoggedOutV2(userReference) =>
        state = state match {
          case ub: LoggedInState => userLoggerOut(ub, userReference)
          case _                 => state
        }
      case _ =>
    }
  }

  override def restoreFromSnapshot(metadata: SnapshotMetadata,
                                   state: State): Unit = {
    state match {
      case s: LoggedInState => this.state = s
    }
  }

  def userLoggerOut(state: LoggedInState,
                    userReference: UserReference): LoggedInState = {
    val users = state.loggedInUsers.filter(_ != userReference)
    state.copy(loggedInUsers = users)
  }

  def userLoggerIn(state: LoggedInState,
                   userReference: UserReference): LoggedInState = {
    val users = state.loggedInUsers + userReference
    state.copy(loggedInUsers = users)
  }

  override val receiveCommand: Receive = LoggingReceive(defaultReceive.orElse {
    // simple persist all event
    case e: UserLoggedInV2 =>
      persist(e)(afterEventPersisted)
    case e: UserLoggedOutV2 =>
      persist(e)(afterEventPersisted)
    case GetState =>
      sender() ! state
    case Initialize(state) =>
      this.state = state
    case RestoreViewFromStateSuccess =>
    case _: StartAggregate           =>
  })
}
