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

@RunWith(classOf[JUnitRunner])
class LoginStateAggregateSpec extends ActorSpecs {

  val actorRef = system.actorOf(LoginStateAggregate.props)
  //system.eventStream.subscribe(testActor, classOf[LoggedInState])

  "LoginStateggregate updateState" should {
    "user login" in {
      val userId = UserId("user1")
      val userId2 = UserId("user2")

      actorRef ! UserLoggedIn(userId)
      expectMsgPF() {
        case LoggedInState(loggedInUsers) =>
          loggedInUsers must contain(userId)
          loggedInUsers must not contain (userId2)
      }
    }

    "user logout" in {
      val userId = UserId("user1")
      val userId2 = UserId("user2")

      actorRef ! UserLoggedIn(userId)
      expectMsg(LoggedInState)
      actorRef ! UserLoggedIn(userId2)
      expectMsg(LoggedInState)
      actorRef ! UserLoggedOut(userId)
      expectMsgPF() {
        case LoggedInState(loggedInUsers) =>
          loggedInUsers must not contain (userId)
          loggedInUsers must contain(userId2)
      }
    }
  }

  shutdown(system)
}