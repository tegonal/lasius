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
package domain

import models.UserId
import akka.persistence._
import akka.actor._
import akka.event.LoggingReceive
import models.PersistetEvent
import models._

object LoginStateAggregate {
  import AggregateRoot._

  case class LoggedInState(loggedInUsers: Seq[UserId]) extends State

  def props: Props = Props(classOf[LoginStateAggregate])

  val persistenceId: String = "login-state"
}

class LoginStateAggregate extends AggregateRoot {
  import AggregateRoot._
  import LoginStateAggregate._

  override def persistenceId: String = LoginStateAggregate.persistenceId

  override var state: State = LoggedInState(Seq())

  override def updateState(evt: PersistetEvent): Unit = {
    evt match {
      case UserLoggedIn(userId) =>
        state = state match {
          case ub: LoggedInState => userLoggerIn(ub, userId)
          case _ => state
        }
      case UserLoggedOut(userId) =>
        state = state match {
          case ub: LoggedInState => userLoggerOut(ub, userId)
          case _ => state
        }
    }
  }

  override def restoreFromSnapshot(metadata: SnapshotMetadata, state: State) = {
    state match {
      case s: LoggedInState => this.state = s
    }
  }

  def userLoggerOut(state: LoggedInState, userId: UserId) = {
    val users = state.loggedInUsers.filter(_ != userId)
    state.copy(loggedInUsers = users)
  }

  def userLoggerIn(state: LoggedInState, userId: UserId) = {
    val users = state.loggedInUsers :+ userId
    state.copy(loggedInUsers = users)
  }

  override val receiveCommand: Receive = LoggingReceive({
    //simple persist all event
    case e: UserLoggedIn =>
      persist(e)(afterEventPersisted)
    case e: UserLoggedOut =>
      persist(e)(afterEventPersisted)
    case GetState =>
      sender ! state
    case Initialize(state) =>
      this.state = state
  })
}