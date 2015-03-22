package domain

import models.UserId
import akka.persistence._
import akka.actor._

object LoginStateAggregate {
  import AggregateRoot._

  case class LoggedInState(loggedInUsers: Seq[UserId]) extends State

  case class UserLoggedIn(userId: UserId) extends Event
  case class UserLoggedOut(userId: UserId) extends Event

  def props: Props = Props(new LoginStateAggregate())

  val persistenceId: String = "login-state"
}

class LoginStateAggregate extends AggregateRoot {
  import AggregateRoot._
  import LoginStateAggregate._

  override def persistenceId: String = LoginStateAggregate.persistenceId

  override var state: State = LoggedInState(Seq())

  override def updateState(evt: Event): Unit = {
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

  override val receiveCommand: Receive = {
    //simple persist all event
    case e: UserLoggedIn =>
      persist(e)(afterEventPersisted)
    case e: UserLoggedOut =>
      persist(e)(afterEventPersisted)
    case GetState =>
      sender() ! state
  }
}