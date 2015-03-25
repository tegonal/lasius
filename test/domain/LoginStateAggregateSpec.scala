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
import akka.testkit.TestProbe
import akka.actor.Props
import akka.actor.Actor
import akka.event.LoggingReceive
import akka.testkit.TestActorRef
import org.specs2.matcher.Scope
import akka.PersistentActorTestScope
import akka.PersistentActorSpecification

class LoginStateAggregateSpec extends PersistentActorSpecification {
  
  "LoginStateggregate" should {
    "user login" in new PersistentActorTestScope  {
        
           val probe = TestProbe()
      val actorRef = system.actorOf(LoginStateAggregate.props)

      val userId = UserId("user1")
      val userId2 = UserId("user2")

      actorRef ! Initialize(LoggedInState(Seq()))
      probe.send(actorRef, UserLoggedIn(userId))
      probe.expectMsg(LoggedInState(Seq(userId)))
    }

    "user logout" in new PersistentActorTestScope {
      val probe = TestProbe()
      val actorRef = system.actorOf(LoginStateAggregate.props)

      val userId = UserId("user1")
      val userId2 = UserId("user2")

      actorRef ! Initialize(LoggedInState(Seq(userId, userId2)))
      probe.send(actorRef, UserLoggedOut(userId))
      probe.expectMsg(LoggedInState(Seq(userId2)))
    }
  }
}
