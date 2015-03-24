package domain

import scala.concurrent.duration._
import akka.testkit.TestKit
import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import akka.testkit.DefaultTimeout
import akka.testkit.ImplicitSender
import org.specs2.matcher.Matchers
import org.specs2.mutable.Specification
import akka.testkit.TestKitBase
import models.UserId
import domain.LoginStateAggregate._
import org.specs2.runner.JUnitRunner
import domain.AggregateRoot._
import org.junit.runner.RunWith
import org.specs2.time.NoTimeConversions
import akka.ActorSpecs
import akka.testkit.TestProbe
import akka.actor.Props
import akka.actor.Actor
import akka.event.LoggingReceive
import akka.testkit.TestActorRef
import org.specs2.matcher.Scope

class LoginStateAggregateSpec extends Specification {

  class Actors extends TestKit(ActorSystem("test")) with Scope

  "LoginStateggregate updateState" should {
    "user login" in new Actors {
      val probe = TestProbe()
      val actorRef = system.actorOf(LoginStateAggregate.props)

      val userId = UserId("user1")
      val userId2 = UserId("user2")

      probe.send(actorRef, UserLoggedIn(userId))
      probe.expectMsgPF() {
        case LoggedInState(loggedInUsers) =>
          loggedInUsers must contain(userId)
          loggedInUsers must not contain (userId2)
      }
    }

    "user logout" in new Actors {
      val probe = TestProbe()
      val actorRef = system.actorOf(LoginStateAggregate.props)

      val userId = UserId("user1")
      val userId2 = UserId("user2")

      probe.send(actorRef, UserLoggedIn(userId))
      probe.expectMsgType[LoggedInState]
      probe.send(actorRef, UserLoggedIn(userId2))
      probe.expectMsgType[LoggedInState]
      probe.send(actorRef, UserLoggedOut(userId))
      probe.expectMsgPF() {
        case LoggedInState(loggedInUsers) =>
          loggedInUsers must not contain (userId)
          loggedInUsers must contain(userId2)
      }
    }
  }
}
