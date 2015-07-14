package domain.views

import org.specs2.mutable.Specification
import org.specs2.mock.Mockito
import akka.PersistentActorTestScope
import models._
import akka.testkit._
import akka.actor._
import org.joda.time.DateTime
import org.joda.time.Duration
import domain.UserTimeBookingAggregate._
import actors.ClientReceiver
import org.mockito.Matchers.{ argThat, anyInt, eq => isEq }
import actors.ClientReceiverComponent

class CurrentUserTimeBookingsViewSpec extends Specification with Mockito {

  "CurrentUserTimeBookingsView UserTimeBookingStarted" should {
    "booking to current state" in new PersistentActorTestScope {

      val userId = UserId("noob")
      val probe = TestProbe()
      val clientReceiver = mock[ClientReceiver]
      val actorRef = system.actorOf(CurrentUserTimeBookingsViewMock.props(userId, clientReceiver))

      val day = DateTime.parse("2000-01-01")
      val start = DateTime.now().minusHours(2)
      val categoryId = CategoryId("cat")
      val projectId = ProjectId("proj")
      val tagId1 = TagId("tag1")
      val tagId2 = TagId("tag2")
      val duration = Duration.standardHours(2)

      val booking = Booking(BookingId("b1"), start, None, userId, categoryId, projectId, Seq(tagId1, tagId2))

      val state = CurrentUserTimeBooking(userId, Some(booking), None, Duration.ZERO)

      probe.send(actorRef, UserTimeBookingStarted(booking))
      probe.expectMsg(CurrentUserTimeBookingsView.Ack)

      there was one(clientReceiver) ! (isEq(userId), isEq(state), isEq(List(userId)))
    }
  }
}

object CurrentUserTimeBookingsViewMock {
  def props(userId: UserId, clientReceiver: ClientReceiver) = Props(classOf[CurrentUserTimeBookingsViewMock], userId, clientReceiver)
}

class CurrentUserTimeBookingsViewMock(userId: UserId, val clientReceiver: ClientReceiver) extends CurrentUserTimeBookingsView(userId)
  with ClientReceiverComponent