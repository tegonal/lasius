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
import domain.UserTimeBookingAggregate._
import models._
import org.joda.time.DateTime
import domain.AggregateRoot.Initialize

class UserTimeBookingAggregateSpec extends Specification {

  class Actors extends TestKit(ActorSystem("test")) with Scope

  "UserTimeBookingAggregate RemoveBooking" should {
    "remove existing booking" in new Actors {
      val probe = TestProbe()
      val stream = TestProbe()
      val userId = UserId("noob")
      val actorRef = system.actorOf(UserTimeBookingAggregate.props(userId))
      system.eventStream.subscribe(stream.ref, classOf[UserTimeBookingRemoved])

      val booking = Booking(BookingId("1"), DateTime.now(), None, userId, CategoryId("cat"), ProjectId("proj"), Seq())

      actorRef ! Initialize(UserTimeBooking(userId, Seq(booking)))
      probe.send(actorRef, RemoveBooking(userId, booking.id))
      probe.expectMsg(UserTimeBooking(userId, Seq()))
      stream.expectMsg(UserTimeBookingRemoved(booking))
    }

    "not publish event if booking does not exist" in new Actors {
      val probe = TestProbe()
      val stream = TestProbe()
      val userId = UserId("noob")
      val actorRef = system.actorOf(UserTimeBookingAggregate.props(userId))

      system.eventStream.subscribe(stream.ref, classOf[UserTimeBookingRemoved])

      actorRef ! Initialize(UserTimeBooking(userId, Seq()))
      probe.send(actorRef, RemoveBooking(userId, BookingId("1")))
      probe.expectNoMsg
      stream.expectNoMsg
    }
  }
}
