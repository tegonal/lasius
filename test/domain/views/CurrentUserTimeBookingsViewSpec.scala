package domain.views

import actors.{ClientReceiver, ClientReceiverComponent}
import akka.PersistentActorTestScope
import akka.actor._
import akka.testkit._
import models._
import org.joda.time.{DateTime, Duration}
import org.specs2.mutable._
import org.specs2.mock._

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

      val state = CurrentUserTimeBookingEvent(CurrentUserTimeBooking(userId, day, Some(booking), None, Duration.ZERO))

      probe.send(actorRef, UserTimeBookingStarted(booking))
      probe.expectMsg(CurrentUserTimeBookingsView.Ack)

      there was one(clientReceiver) ! (org.mockito.ArgumentMatchers.eq(userId), org.mockito.ArgumentMatchers.eq(state), org.mockito.ArgumentMatchers.eq(List(userId)))
    }
  }

  "CurrentUserTimeBookingsView UserTimeBookingStartTimeChanged" should {
    "adjust time of current booking" in new PersistentActorTestScope {

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
      val newStart = start.minusHours(2)

      val booking = Booking(BookingId("b1"), start, None, userId, categoryId, projectId, Seq(tagId1, tagId2))

      //first start booking
      probe.send(actorRef, UserTimeBookingStarted(booking))
      probe.expectMsg(CurrentUserTimeBookingsView.Ack)

      //then move start time
      probe.send(actorRef, UserTimeBookingStartTimeChanged(booking.id, start, newStart))
      probe.expectMsg(CurrentUserTimeBookingsView.Ack)

      val newBooking = booking.copy(start = newStart)
      val state = CurrentUserTimeBookingEvent(CurrentUserTimeBooking(userId, day, Some(newBooking), None, Duration.ZERO))
      there was one(clientReceiver) ! (org.mockito.ArgumentMatchers.eq(userId), org.mockito.ArgumentMatchers.eq(state), org.mockito.ArgumentMatchers.eq(List(userId)))
    }
  }

  "CurrentUserTimeBookingsView UserTimeBookingEdited" should {
    "adjusted daily total of booking when editing booking" in new PersistentActorTestScope {

      val userId = UserId("noob")
      val probe = TestProbe()
      val clientReceiver = mock[ClientReceiver]
      val actorRef = system.actorOf(CurrentUserTimeBookingsViewMock.props(userId, clientReceiver))

      val day = DateTime.parse("2000-01-01")
      val end = DateTime.now()
      val start = end.minusHours(2)
      val categoryId = CategoryId("cat")
      val projectId = ProjectId("proj")
      val tagId1 = TagId("tag1")
      val tagId2 = TagId("tag2")
      val duration = Duration.standardHours(2)

      val booking = Booking(BookingId("b1"), start, Some(end), userId, categoryId, projectId, Seq(tagId1, tagId2))

      val state = CurrentUserTimeBookingEvent(CurrentUserTimeBooking(userId, day, None, None, duration))

      probe.send(actorRef, UserTimeBookingStopped(booking))
      probe.expectMsg(CurrentUserTimeBookingsView.Ack)

      there was one(clientReceiver) ! (org.mockito.ArgumentMatchers.eq(userId), org.mockito.ArgumentMatchers.eq(state), org.mockito.ArgumentMatchers.eq(List(userId)))

      //edit time booking
      val newDuration = Duration.standardHours(4)
      //expect new duration of current booking, without booking in progress
      val newState = CurrentUserTimeBookingEvent(CurrentUserTimeBooking(userId, day, None, None, newDuration))

      val newStart = start.minusHours(2)
      probe.send(actorRef, UserTimeBookingEdited(booking, newStart, end))
      probe.expectMsg(CurrentUserTimeBookingsView.Ack)

      there was one(clientReceiver) ! (org.mockito.ArgumentMatchers.eq(userId), org.mockito.ArgumentMatchers.eq(newState), org.mockito.ArgumentMatchers.eq(List(userId)))
    }
  }
}

object CurrentUserTimeBookingsViewMock {
  def props(userId: UserId, clientReceiver: ClientReceiver) = Props(classOf[CurrentUserTimeBookingsViewMock], userId, clientReceiver)
}

class CurrentUserTimeBookingsViewMock(userId: UserId, val clientReceiver: ClientReceiver) extends CurrentUserTimeBookingsView(userId)
  with ClientReceiverComponent