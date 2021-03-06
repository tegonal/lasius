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

import actor.ClientReceiverComponentMock

import scala.concurrent.duration._
import akka.actor._
import com.typesafe.config.ConfigFactory
import akka.testkit._
import org.specs2.matcher.Matchers
import org.mockito.Matchers.{ argThat, anyInt, eq => isEq }
import org.specs2.mutable.Specification
import org.specs2.mock.Mockito
import akka.testkit.TestKitBase
import models.UserId
import org.specs2.runner.JUnitRunner
import domain.AggregateRoot._
import org.junit.runner.RunWith
import org.specs2.time.NoTimeConversions
import akka.testkit.TestProbe
import akka.event.LoggingReceive
import akka.testkit.TestActorRef
import org.specs2.matcher.Scope
import domain.UserTimeBookingAggregate._
import models._
import org.joda.time.DateTime
import domain.AggregateRoot.Initialize
import akka._
import akka.PersistentActorTestScope
import mongo.EmbedMongo
import scala.concurrent.Await
import org.specs2.execute.Result
import repositories._
import repositories.UserBookingHistoryRepositoryComponent
import scala.concurrent._
import play.api.test._

class UserTimeBookingAggregateSpec extends Specification with Mockito {

  "UserTimeBookingAggregate RemoveBooking" should {
    "remove existing booking" in new PersistentActorTestScope {
      implicit val context = scala.concurrent.ExecutionContext.Implicits.global
      val probe = TestProbe()
      val stream = TestProbe()
      val userId = UserId("noob")
      val component = new UserBookingHistoryRepositoryComponentMock
      val actorRef = system.actorOf(UserTimeBookingAggregateMock.props(userId, component))
      system.eventStream.subscribe(stream.ref, classOf[Any])

      val booking = Booking(BookingId("1"), DateTime.now(), None, userId, CategoryId("cat"), ProjectId("proj"), Seq())

      actorRef ! Initialize(UserTimeBooking(userId, Seq(booking)))

      //execute
      probe.send(actorRef, RemoveBooking(userId, booking.id))

      //verify
      probe.expectMsg(UserTimeBooking(userId, Seq()))
      stream.expectMsg(UserTimeBookingRemoved(booking))

      there was one(component.bookingHistoryRepository).remove(booking)
    }

    "not publish event if booking does not exist" in new PersistentActorTestScope {
      val probe = TestProbe()
      val stream = TestProbe()
      val userId = UserId("noob")
      val component = new UserBookingHistoryRepositoryComponentMock
      val actorRef = system.actorOf(UserTimeBookingAggregateMock.props(userId, component))

      system.eventStream.subscribe(stream.ref, classOf[Any])

      actorRef ! Initialize(UserTimeBooking(userId, Seq()))
      //execute
      probe.send(actorRef, RemoveBooking(userId, BookingId("1")))

      //verify
      probe.expectNoMsg
      stream.expectNoMsg

      there was no(component.bookingHistoryRepository).remove(any[Booking])(any[ExecutionContext])
    }
  }

  "UserTimeBookingAggregate AddBooking" should {
    "Stop currently running booking" in new PersistentActorTestScope {
      val probe = TestProbe()
      val stream = TestProbe()
      val userId = UserId("noob")
      val component = new UserBookingHistoryRepositoryComponentMock
      val actorRef = system.actorOf(UserTimeBookingAggregateMock.props(userId, component))

      system.eventStream.subscribe(stream.ref, classOf[Any])
      val currentBooking = Booking(BookingId("1"), DateTime.now.minusHours(2), None, userId, CategoryId("cat"), ProjectId("proj"), Seq())
      val newBooking = Booking(BookingId("2"), DateTime.now, None, userId, CategoryId("cat"), ProjectId("proj"), Seq())
      val closedBooking = currentBooking.copy(end = Some(newBooking.start))

      actorRef ! Initialize(UserTimeBooking(userId, Seq(currentBooking)))

      //execute
      probe.send(actorRef, StartBooking(userId, newBooking.categoryId, newBooking.projectId,
        newBooking.tags, newBooking.start))

      //verify
      probe.expectMsg(UserTimeBooking(userId, Seq(closedBooking)))
      probe.expectMsgPF() {
        case UserTimeBooking(userId, bookings) =>
          bookings must haveSize(2)
          bookings(0) must beEqualTo(closedBooking)
          bookings(1).start must beEqualTo(newBooking.start)
          bookings(1).categoryId must beEqualTo(newBooking.categoryId)
          bookings(1).projectId must beEqualTo(newBooking.projectId)
          bookings(1).tags must beEqualTo(newBooking.tags)
      }
      stream.expectMsg(UserTimeBookingStopped(closedBooking))
      stream.expectMsgPF() {
        case UserTimeBookingStarted(booking) =>
          booking.start must beEqualTo(newBooking.start)
          booking.categoryId must beEqualTo(newBooking.categoryId)
          booking.projectId must beEqualTo(newBooking.projectId)
          booking.tags must beEqualTo(newBooking.tags)
      }

      //add current booking to repository
      there was one(component.bookingHistoryRepository).insert(isEq(closedBooking))(any[ExecutionContext])
    }

    "Start new booking" in new PersistentActorTestScope {
      val probe = TestProbe()
      val stream = TestProbe()
      val userId = UserId("noob")
      val component = new UserBookingHistoryRepositoryComponentMock
      val actorRef = system.actorOf(UserTimeBookingAggregateMock.props(userId, component))

      system.eventStream.subscribe(stream.ref, classOf[Any])
      val newBooking = Booking(BookingId("2"), DateTime.now, None, userId, CategoryId("cat"), ProjectId("proj"), Seq())

      actorRef ! Initialize(UserTimeBooking(userId, Seq()))

      //execute
      probe.send(actorRef, StartBooking(userId, newBooking.categoryId, newBooking.projectId,
        newBooking.tags, newBooking.start))

      //verify
      probe.expectMsgPF() {
        case UserTimeBooking(userId, bookings) =>
          bookings must haveSize(1)
          bookings(0).start must beEqualTo(newBooking.start)
          bookings(0).categoryId must beEqualTo(newBooking.categoryId)
          bookings(0).projectId must beEqualTo(newBooking.projectId)
          bookings(0).tags must beEqualTo(newBooking.tags)
      }
      stream.expectMsgPF() {
        case UserTimeBookingStarted(booking) =>
          booking.start must beEqualTo(newBooking.start)
          booking.categoryId must beEqualTo(newBooking.categoryId)
          booking.projectId must beEqualTo(newBooking.projectId)
          booking.tags must beEqualTo(newBooking.tags)
      }
    }
  }

  "UserTimeBookingAggregate EndBooking" should {
    "don't stop booking if not the same id" in new PersistentActorTestScope {
      val probe = TestProbe()
      val stream = TestProbe()
      val userId = UserId("noob")
      val component = new UserBookingHistoryRepositoryComponentMock
      val actorRef = system.actorOf(UserTimeBookingAggregateMock.props(userId, component))

      system.eventStream.subscribe(stream.ref, classOf[UserTimeBookingRemoved])
      val currentBooking = Booking(BookingId("1"), DateTime.now.minusHours(2), None, userId, CategoryId("cat"), ProjectId("proj"), Seq())

      actorRef ! Initialize(UserTimeBooking(userId, Seq(currentBooking)))

      //execute
      probe.send(actorRef, EndBooking(userId, BookingId("2"), DateTime.now))

      //verify
      probe.expectNoMsg
      stream.expectNoMsg

      //add current booking to repository
      there was no(component.bookingHistoryRepository).insert(any[Booking])(any[ExecutionContext])
    }

    "stop booking with provided enddate" in new PersistentActorTestScope {
      val probe = TestProbe()
      val stream = TestProbe()
      val userId = UserId("noob")
      val component = new UserBookingHistoryRepositoryComponentMock
      val actorRef = system.actorOf(UserTimeBookingAggregateMock.props(userId, component))

      system.eventStream.subscribe(stream.ref, classOf[Any])
      val currentBooking = Booking(BookingId("1"), DateTime.now.minusHours(2), None, userId, CategoryId("cat"), ProjectId("proj"), Seq())
      var date = DateTime.now
      val closedBooking = currentBooking.copy(end = Some(date))

      actorRef ! Initialize(UserTimeBooking(userId, Seq(currentBooking)))

      //execute
      probe.send(actorRef, EndBooking(userId, currentBooking.id, date))

      //verify
      probe.expectMsg(UserTimeBooking(userId, Seq(closedBooking)))
      stream.expectMsg(UserTimeBookingStopped(closedBooking))

      //add current booking to repository
      there was one(component.bookingHistoryRepository).insert(isEq(closedBooking))(any[ExecutionContext])
    }

    "stop booking with enddate in future" in new PersistentActorTestScope {
      val probe = TestProbe()
      val stream = TestProbe()
      val userId = UserId("noob")
      val component = new UserBookingHistoryRepositoryComponentMock
      val actorRef = system.actorOf(UserTimeBookingAggregateMock.props(userId, component))

      system.eventStream.subscribe(stream.ref, classOf[Any])
      val currentBooking = Booking(BookingId("1"), DateTime.now.minusHours(2), None, userId, CategoryId("cat"), ProjectId("proj"), Seq())
      var date = DateTime.now.plusHours(2)
      val closedBooking = currentBooking.copy(end = Some(date))

      actorRef ! Initialize(UserTimeBooking(userId, Seq(currentBooking)))

      //execute
      probe.send(actorRef, EndBooking(userId, currentBooking.id, date))

      //verify
      probe.expectMsg(UserTimeBooking(userId, Seq(closedBooking)))
      stream.expectMsg(UserTimeBookingStopped(closedBooking))

      //add current booking to repository
      there was one(component.bookingHistoryRepository).insert(isEq(closedBooking))(any[ExecutionContext])
    }
  }

  "UserTimeBookingAggregate UserTimeBookingEdited" should {
    "Update user time booking" in new PersistentActorTestScope {
      val probe = TestProbe()
      val stream = TestProbe()
      val userId = UserId("noob")
      val component = new UserBookingHistoryRepositoryComponentMock
      val actorRef = system.actorOf(UserTimeBookingAggregateMock.props(userId, component))

      system.eventStream.subscribe(stream.ref, classOf[Any])
      val end = DateTime.now()
      val start = end.minusHours(2)
      val newStart = start.minusHours(2)
      val currentBooking = Booking(BookingId("1"), start, Some(end), userId, CategoryId("cat"), ProjectId("proj"), Seq())
      val modifiedBooking = currentBooking.copy(start = newStart)

      component.bookingHistoryRepository.updateTimeBooking(isEq(currentBooking.id), isEq(newStart), isEq(end)) returns Future.successful(true)

      actorRef ! Initialize(UserTimeBooking(userId, Seq(currentBooking)))

      //execute
      probe.send(actorRef, EditBooking(userId, currentBooking.id, newStart, end))

      //verify
      probe.expectMsg(UserTimeBooking(userId, Seq(modifiedBooking)))
      stream.expectMsg(UserTimeBookingEdited(currentBooking, newStart, end))

      there was one(component.bookingHistoryRepository).updateTimeBooking(isEq(currentBooking.id), isEq(newStart), isEq(end))
    }
  }

  "UserTimeBookingAggregate UserTimeBookingStartTimeChanged" should {
    "Move start time of booking in progress" in new PersistentActorTestScope {
      val probe = TestProbe()
      val stream = TestProbe()
      val userId = UserId("noob")
      val component = new UserBookingHistoryRepositoryComponentMock
      val actorRef = system.actorOf(UserTimeBookingAggregateMock.props(userId, component))
      val start = DateTime.now.minusHours(2)
      val newStart = start.minusHours(4)

      system.eventStream.subscribe(stream.ref, classOf[Any])
      val currentBooking = Booking(BookingId("1"), start, None, userId, CategoryId("cat"), ProjectId("proj"), Seq())
      val adjustedBooking = currentBooking.copy(start = newStart)

      actorRef ! Initialize(UserTimeBooking(userId, Seq(currentBooking)))

      //execute
      probe.send(actorRef, ChangeStartTimeOfBooking(userId, currentBooking.id, newStart))

      //verify
      probe.expectMsg(UserTimeBooking(userId, Seq(adjustedBooking)))
      stream.expectMsg(UserTimeBookingStartTimeChanged(adjustedBooking.id, start, newStart))
    }

    "do nothing if booking is not in progress" in new PersistentActorTestScope {
      val probe = TestProbe()
      val stream = TestProbe()
      val userId = UserId("noob")
      val component = new UserBookingHistoryRepositoryComponentMock
      val actorRef = system.actorOf(UserTimeBookingAggregateMock.props(userId, component))
      val start = DateTime.now.minusHours(2)
      val end = start.plusHours(3)
      val newStart = start.minusHours(4)

      system.eventStream.subscribe(stream.ref, classOf[Any])
      val currentBooking = Booking(BookingId("1"), start, Some(end), userId, CategoryId("cat"), ProjectId("proj"), Seq())

      actorRef ! Initialize(UserTimeBooking(userId, Seq(currentBooking)))

      //execute
      probe.send(actorRef, ChangeStartTimeOfBooking(userId, currentBooking.id, newStart))

      //verify
      probe.expectNoMsg
      stream.expectNoMsg
    }
  }
}

object UserTimeBookingAggregateMock {

  def props(userId: UserId, comp: UserBookingHistoryRepositoryComponentMock) = Props(classOf[UserTimeBookingAggregateMock], userId,
    comp.bookingHistoryRepository)
}

class UserTimeBookingAggregateMock(userId: UserId, val bookingHistoryRepository: BookingHistoryRepository) extends UserTimeBookingAggregate(userId)
  with UserBookingHistoryRepositoryComponent with ClientReceiverComponentMock
