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

import actors.ClientReceiver
import akka.PersistentActorTestScope
import akka.actor._
import akka.testkit.TestProbe
import core.{DBSession, MockServices, SystemServices}
import domain.AggregateRoot.Initialize
import domain.UserTimeBookingAggregate._
import models.LocalDateTimeWithTimeZone.DateTimeHelper
import models._
import mongo.EmbedMongo
import org.joda.time.DateTime
import org.mockito.ArgumentMatchers
import org.specs2.mock._
import org.specs2.mock.mockito.MockitoMatchers
import org.specs2.mutable._
import play.api.libs.json.{Format, Writes}
import play.modules.reactivemongo.ReactiveMongoApi
import repositories._
import util.MockAwaitable

import scala.concurrent._

class UserTimeBookingAggregateSpec
    extends Specification
    with Mockito
    with MockAwaitable
    with MockitoMatchers
    with EmbedMongo {

  val clientReceiver = mock[ClientReceiver]

  "UserTimeBookingAggregate RemoveBooking" should {
    "remove existing booking" in new PersistentActorTestScope {
      val systemServices = new MockServices(system)
      val probe          = TestProbe()
      val stream         = TestProbe()
      val userReference =
        EntityReference(UserId(), "noob")
      val projectReference =
        EntityReference(ProjectId(), "proj")
      val teamReference =
        EntityReference(OrganisationId(), "team")

      val bookingHistoryRepository = mockAwaitable[BookingHistoryRepository]
      val actorRef =
        system.actorOf(
          UserTimeBookingAggregateMock.props(systemServices,
                                             clientReceiver,
                                             bookingHistoryRepository,
                                             userReference,
                                             reactiveMongoApi))
      system.eventStream.subscribe(stream.ref, classOf[PersistedEvent])

      val booking = BookingV2(BookingId(),
                              DateTime.now().toLocalDateTimeWithZone(),
                              None,
                              userReference,
                              teamReference,
                              projectReference,
                              Set())

      actorRef ! Initialize(UserTimeBooking(userReference, Seq(booking)))

      // execute
      probe.send(actorRef,
                 RemoveBookingCommand(userReference,
                                      booking.organisationReference,
                                      booking.id))

      // verify
      probe.expectMsg(UserTimeBooking(userReference, Seq()))
      stream.expectMsg(UserTimeBookingRemovedV2(booking))

      there.was(
        one(bookingHistoryRepository).remove(ArgumentMatchers.eq(booking))(
          any[DBSession]))
    }

    "not publish event if booking does not exist" in new PersistentActorTestScope {
      val systemServices = new MockServices(system)
      val probe          = TestProbe()
      val stream         = TestProbe()
      val userReference =
        EntityReference(UserId(), "noob")
      val bookingHistoryRepository = mockAwaitable[BookingHistoryRepository]
      val actorRef =
        system.actorOf(
          UserTimeBookingAggregateMock.props(systemServices,
                                             clientReceiver,
                                             bookingHistoryRepository,
                                             userReference,
                                             reactiveMongoApi))

      system.eventStream.subscribe(stream.ref, classOf[PersistedEvent])
      val organisationReference = EntityReference(OrganisationId(), "")

      actorRef ! Initialize(UserTimeBooking(userReference, Seq()))
      // execute
      probe.send(
        actorRef,
        RemoveBookingCommand(userReference, organisationReference, BookingId()))

      // verify
      probe.expectNoMessage()
      stream.expectNoMessage()

      there.was(
        no(bookingHistoryRepository)
          .remove(any[BookingV2])(any[DBSession]))
    }
  }

  "UserTimeBookingAggregate AddBooking" should {
    "Stop currently running booking" in new PersistentActorTestScope {
      val systemServices = new MockServices(system)
      val probe          = TestProbe()
      val stream         = TestProbe()
      val userReference =
        EntityReference(UserId(), "noob")
      val projectReference =
        EntityReference(ProjectId(), "proj")
      val teamReference =
        EntityReference(OrganisationId(), "team")
      val bookingHistoryRepository = mockAwaitable[BookingHistoryRepository]
      val actorRef =
        system.actorOf(
          UserTimeBookingAggregateMock.props(systemServices,
                                             clientReceiver,
                                             bookingHistoryRepository,
                                             userReference,
                                             reactiveMongoApi))

      system.eventStream.subscribe(stream.ref, classOf[PersistedEvent])

      val currentBooking =
        BookingV2(BookingId(),
                  DateTime.now.minusHours(2).toLocalDateTimeWithZone(),
                  None,
                  userReference,
                  teamReference,
                  projectReference,
                  Set())
      val newBooking = BookingV2(BookingId(),
                                 DateTime.now.toLocalDateTimeWithZone(),
                                 None,
                                 userReference,
                                 teamReference,
                                 projectReference,
                                 Set())
      val closedBooking = currentBooking.copy(end = Some(newBooking.start))

      actorRef ! Initialize(UserTimeBooking(userReference, Seq(currentBooking)))

      // execute
      probe.send(
        actorRef,
        StartBookingCommand(userReference,
                            newBooking.organisationReference,
                            newBooking.projectReference,
                            newBooking.tags,
                            newBooking.start.toDateTime())
      )

      // verify
      probe.expectMsg(UserTimeBooking(userReference, Seq(closedBooking)))
      probe.expectMsgPF() { case UserTimeBooking(_, bookings) =>
        bookings must haveSize(2)
        bookings(0) must beEqualTo(closedBooking)
        bookings(1).start must beEqualTo(newBooking.start)
        bookings(1).projectReference must beEqualTo(newBooking.projectReference)
        bookings(1).organisationReference must beEqualTo(
          newBooking.organisationReference)
        bookings(1).tags must beEqualTo(newBooking.tags)
      }
      stream.expectMsg(UserTimeBookingStoppedV2(closedBooking))
      stream.expectMsgPF() { case UserTimeBookingStartedV2(booking) =>
        booking.start must beEqualTo(newBooking.start)
        booking.projectReference must beEqualTo(newBooking.projectReference)
        booking.organisationReference must beEqualTo(
          newBooking.organisationReference)
        booking.tags must beEqualTo(newBooking.tags)
      }

      // add current booking to repository
      there.was(
        one(bookingHistoryRepository)
          .upsert(ArgumentMatchers.eq(closedBooking))(any[Writes[BookingId]],
                                                      any[DBSession]))
    }

    "Start new booking" in new PersistentActorTestScope {
      val systemServices = new MockServices(system)
      val probe          = TestProbe()
      val stream         = TestProbe()
      val userReference =
        EntityReference(UserId(), "noob")
      val projectReference =
        EntityReference(ProjectId(), "proj")
      val teamReference =
        EntityReference(OrganisationId(), "team")
      val bookingHistoryRepository = mockAwaitable[BookingHistoryRepository]
      val actorRef =
        system.actorOf(
          UserTimeBookingAggregateMock.props(systemServices,
                                             clientReceiver,
                                             bookingHistoryRepository,
                                             userReference,
                                             reactiveMongoApi))

      system.eventStream.subscribe(stream.ref, classOf[PersistedEvent])

      val newBooking = BookingV2(BookingId(),
                                 DateTime.now.toLocalDateTimeWithZone(),
                                 None,
                                 userReference,
                                 teamReference,
                                 projectReference,
                                 Set())

      actorRef ! Initialize(UserTimeBooking(userReference, Seq()))

      // execute
      probe.send(
        actorRef,
        StartBookingCommand(userReference,
                            newBooking.organisationReference,
                            newBooking.projectReference,
                            newBooking.tags,
                            newBooking.start.toDateTime())
      )

      // verify
      probe.expectMsgPF() { case UserTimeBooking(_, bookings) =>
        bookings must haveSize(1)
        bookings(0).start must beEqualTo(newBooking.start)
        bookings(0).organisationReference must beEqualTo(
          newBooking.organisationReference)
        bookings(0).projectReference must beEqualTo(newBooking.projectReference)
        bookings(0).tags must beEqualTo(newBooking.tags)
      }
      stream.expectMsgPF() { case UserTimeBookingStartedV2(booking) =>
        booking.start must beEqualTo(newBooking.start)
        booking.organisationReference must beEqualTo(
          newBooking.organisationReference)
        booking.projectReference must beEqualTo(newBooking.projectReference)
        booking.tags must beEqualTo(newBooking.tags)
      }
    }
  }

  "UserTimeBookingAggregate EndBooking" should {
    "don't stop booking if not the same id" in new PersistentActorTestScope {
      val systemServices = new MockServices(system)
      val probe          = TestProbe()
      val stream         = TestProbe()
      val userReference =
        EntityReference(UserId(), "noob")
      val projectReference =
        EntityReference(ProjectId(), "proj")
      val teamReference =
        EntityReference(OrganisationId(), "team")
      val bookingHistoryRepository = mockAwaitable[BookingHistoryRepository]
      val actorRef =
        system.actorOf(
          UserTimeBookingAggregateMock.props(systemServices,
                                             clientReceiver,
                                             bookingHistoryRepository,
                                             userReference,
                                             reactiveMongoApi))

      system.eventStream.subscribe(stream.ref,
                                   classOf[UserTimeBookingRemovedV2])
      val currentBooking =
        BookingV2(BookingId(),
                  DateTime.now.minusHours(2).toLocalDateTimeWithZone(),
                  None,
                  userReference,
                  teamReference,
                  projectReference,
                  Set())

      actorRef ! Initialize(UserTimeBooking(userReference, Seq(currentBooking)))

      // execute
      probe.send(actorRef,
                 EndBookingCommand(userReference,
                                   teamReference,
                                   BookingId(),
                                   DateTime.now))

      // verify
      probe.expectNoMessage()
      stream.expectNoMessage()

      // add current booking to repository
      there.was(
        no(bookingHistoryRepository)
          .bulkInsert(any[List[BookingV2]])(any[DBSession]))
    }

    "stop booking with provided enddate" in new PersistentActorTestScope {
      val systemServices = new MockServices(system)
      val probe          = TestProbe()
      val stream         = TestProbe()
      val userReference =
        EntityReference(UserId(), "noob")
      val projectReference =
        EntityReference(ProjectId(), "proj")
      val teamReference =
        EntityReference(OrganisationId(), "team")
      val bookingHistoryRepository = mockAwaitable[BookingHistoryRepository]
      val actorRef =
        system.actorOf(
          UserTimeBookingAggregateMock.props(systemServices,
                                             clientReceiver,
                                             bookingHistoryRepository,
                                             userReference,
                                             reactiveMongoApi))

      system.eventStream.subscribe(stream.ref, classOf[PersistedEvent])

      val currentBooking =
        BookingV2(BookingId(),
                  DateTime.now.minusHours(2).toLocalDateTimeWithZone(),
                  None,
                  userReference,
                  teamReference,
                  projectReference,
                  Set())
      var date = DateTime.now
      val closedBooking =
        currentBooking.copy(end = Some(date.toLocalDateTimeWithZone()))

      actorRef ! Initialize(UserTimeBooking(userReference, Seq(currentBooking)))

      // execute
      probe.send(actorRef,
                 EndBookingCommand(userReference,
                                   teamReference,
                                   currentBooking.id,
                                   date))

      // verify
      probe.expectMsg(UserTimeBooking(userReference, Seq(closedBooking)))
      stream.expectMsg(UserTimeBookingStoppedV2(closedBooking))

      // add current booking to repository
      there.was(
        one(bookingHistoryRepository)
          .upsert(ArgumentMatchers.eq(closedBooking))(any[Writes[BookingId]],
                                                      any[DBSession]))
    }

    "stop booking with enddate in future" in new PersistentActorTestScope {
      val systemServices = new MockServices(system)
      val probe          = TestProbe()
      val stream         = TestProbe()
      val userReference =
        EntityReference(UserId(), "noob")
      val projectReference =
        EntityReference(ProjectId(), "proj")
      val teamReference =
        EntityReference(OrganisationId(), "team")
      val bookingHistoryRepository = mockAwaitable[BookingHistoryRepository]
      val actorRef =
        system.actorOf(
          UserTimeBookingAggregateMock.props(systemServices,
                                             clientReceiver,
                                             bookingHistoryRepository,
                                             userReference,
                                             reactiveMongoApi))

      system.eventStream.subscribe(stream.ref, classOf[PersistedEvent])
      val currentBooking =
        BookingV2(BookingId(),
                  DateTime.now.minusHours(2).toLocalDateTimeWithZone(),
                  None,
                  userReference,
                  teamReference,
                  projectReference,
                  Set())
      var date = DateTime.now.plusHours(2)
      val closedBooking =
        currentBooking.copy(end = Some(date.toLocalDateTimeWithZone()))

      actorRef ! Initialize(UserTimeBooking(userReference, Seq(currentBooking)))

      // execute
      probe.send(actorRef,
                 EndBookingCommand(userReference,
                                   teamReference,
                                   currentBooking.id,
                                   date))

      // verify
      probe.expectMsg(UserTimeBooking(userReference, Seq(closedBooking)))
      stream.expectMsg(UserTimeBookingStoppedV2(closedBooking))

      // add current booking to repository
      there.was(
        one(bookingHistoryRepository)
          .upsert(ArgumentMatchers.eq(closedBooking))(any[Writes[BookingId]],
                                                      any[DBSession]))
    }
  }

  "UserTimeBookingAggregate UserTimeBookingEdited" should {

    "update currently running booking" in new PersistentActorTestScope {
      val systemServices = new MockServices(system)
      val probe          = TestProbe()
      val stream         = TestProbe()
      val userReference =
        EntityReference(UserId(), "noob")
      val projectReference =
        EntityReference(ProjectId(), "proj")
      val teamReference =
        EntityReference(OrganisationId(), "team")
      val bookingHistoryRepository = mockAwaitable[BookingHistoryRepository]
      val actorRef =
        system.actorOf(
          UserTimeBookingAggregateMock.props(systemServices,
                                             clientReceiver,
                                             bookingHistoryRepository,
                                             userReference,
                                             reactiveMongoApi))

      system.eventStream.subscribe(stream.ref, classOf[PersistedEvent])

      val currentBooking =
        BookingV2(BookingId(),
                  DateTime.now.minusHours(2).toLocalDateTimeWithZone(),
                  None,
                  userReference,
                  teamReference,
                  projectReference,
                  Set())
      val editedBooking = currentBooking.copy(projectReference =
        EntityReference(ProjectId(), "proj2"))
      val editedBookingWithNewHash = editedBooking.copy(bookingHash =
        BookingHash.createHash(editedBooking.projectReference,
                               editedBooking.tags))

      actorRef ! Initialize(UserTimeBooking(userReference, Seq(currentBooking)))

      // execute
      probe.send(
        actorRef,
        EditBookingCommand(
          userReference,
          editedBooking.organisationReference,
          currentBooking.id,
          Some(editedBooking.projectReference),
          Some(editedBooking.tags),
          Some(editedBooking.start.toDateTime()),
          Some(editedBooking.end.map(_.toDateTime()))
        )
      )

      // verify
      probe.expectMsg(
        UserTimeBooking(userReference, Seq(editedBookingWithNewHash)))
      stream.expectMsg(
        UserTimeBookingEditedV3(currentBooking, editedBookingWithNewHash))
    }

    "Update user time booking history" in new PersistentActorTestScope {
      val systemServices = new MockServices(system)
      val probe          = TestProbe()
      val stream         = TestProbe()
      val userReference =
        EntityReference(UserId(), "noob")
      val projectReference =
        EntityReference(ProjectId(), "proj")
      val teamReference =
        EntityReference(OrganisationId(), "team")
      val bookingHistoryRepository = mockAwaitable[BookingHistoryRepository]
      val actorRef =
        system.actorOf(
          UserTimeBookingAggregateMock.props(systemServices,
                                             clientReceiver,
                                             bookingHistoryRepository,
                                             userReference,
                                             reactiveMongoApi))

      system.eventStream.subscribe(stream.ref, classOf[PersistedEvent])
      val end      = DateTime.now()
      val start    = end.minusHours(2)
      val newStart = start.minusHours(2)
      val currentBooking = BookingV2(BookingId(),
                                     start.toLocalDateTimeWithZone(),
                                     Some(end.toLocalDateTimeWithZone()),
                                     userReference,
                                     teamReference,
                                     projectReference,
                                     Set())
      val modifiedBooking =
        currentBooking.copy(start = newStart.toLocalDateTimeWithZone())

      val newBooking =
        currentBooking.copy(start = newStart.toLocalDateTimeWithZone(),
                            end = Some(end.toLocalDateTimeWithZone()))
      bookingHistoryRepository
        .updateBooking(ArgumentMatchers.eq(newBooking))(any[Format[BookingV2]],
                                                        any[DBSession])
        .returns(Future.successful(true))

      actorRef ! Initialize(UserTimeBooking(userReference, Seq(currentBooking)))

      // execute
      probe.send(actorRef,
                 EditBookingCommand(userReference,
                                    currentBooking.organisationReference,
                                    currentBooking.id,
                                    None,
                                    None,
                                    Some(newStart),
                                    Some(Some(end))))

      // verify
      probe.expectMsg(UserTimeBooking(userReference, Seq(modifiedBooking)))
      stream.expectMsg(UserTimeBookingEditedV3(currentBooking, modifiedBooking))

      there.was(
        one(bookingHistoryRepository)
          .updateBooking(ArgumentMatchers.eq(newBooking))(
            any[Format[BookingV2]],
            any[DBSession]))
    }
  }

  "UserTimeBookingAggregate UserTimeBookingStartTimeChanged" should {
    "Move start time of booking in progress" in new PersistentActorTestScope {
      val systemServices = new MockServices(system)
      val probe          = TestProbe()
      val stream         = TestProbe()
      val userReference =
        EntityReference(UserId(), "noob")
      val projectReference =
        EntityReference(ProjectId(), "proj")
      val teamReference =
        EntityReference(OrganisationId(), "team")
      val bookingHistoryRepository = mockAwaitable[BookingHistoryRepository]
      val actorRef =
        system.actorOf(
          UserTimeBookingAggregateMock.props(systemServices,
                                             clientReceiver,
                                             bookingHistoryRepository,
                                             userReference,
                                             reactiveMongoApi))
      val start    = DateTime.now.minusHours(2)
      val newStart = start.minusHours(4)

      system.eventStream.subscribe(stream.ref, classOf[PersistedEvent])
      val currentBooking =
        BookingV2(BookingId(),
                  start.toLocalDateTimeWithZone(),
                  None,
                  userReference,
                  teamReference,
                  projectReference,
                  Set())
      val adjustedBooking =
        currentBooking.copy(start = newStart.toLocalDateTimeWithZone())

      actorRef ! Initialize(UserTimeBooking(userReference, Seq(currentBooking)))

      // execute
      probe.send(actorRef,
                 ChangeStartTimeOfBooking(userReference,
                                          teamReference,
                                          currentBooking.id,
                                          newStart))

      // verify
      probe.expectMsg(UserTimeBooking(userReference, Seq(adjustedBooking)))
      stream.expectMsg(
        UserTimeBookingStartTimeChanged(adjustedBooking.id, start, newStart))
    }

    "do nothing if booking is not in progress" in new PersistentActorTestScope {
      val systemServices = new MockServices(system)
      val probe          = TestProbe()
      val stream         = TestProbe()
      val userReference =
        EntityReference(UserId(), "noob")
      val projectReference =
        EntityReference(ProjectId(), "proj")
      val teamReference =
        EntityReference(OrganisationId(), "team")
      val bookingHistoryRepository = mockAwaitable[BookingHistoryRepository]
      val actorRef =
        system.actorOf(
          UserTimeBookingAggregateMock.props(systemServices,
                                             clientReceiver,
                                             bookingHistoryRepository,
                                             userReference,
                                             reactiveMongoApi))
      val start    = DateTime.now.minusHours(2)
      val end      = start.plusHours(3)
      val newStart = start.minusHours(4)

      system.eventStream.subscribe(stream.ref, classOf[PersistedEvent])
      val currentBooking = BookingV2(BookingId(),
                                     start.toLocalDateTimeWithZone(),
                                     Some(end.toLocalDateTimeWithZone()),
                                     userReference,
                                     teamReference,
                                     projectReference,
                                     Set())

      actorRef ! Initialize(UserTimeBooking(userReference, Seq(currentBooking)))

      // execute
      probe.send(actorRef,
                 ChangeStartTimeOfBooking(userReference,
                                          teamReference,
                                          currentBooking.id,
                                          newStart))

      // verify
      probe.expectNoMessage()
      stream.expectNoMessage()
    }
  }
}

object UserTimeBookingAggregateMock {

  def props(systemServices: SystemServices,
            clientReceiver: ClientReceiver,
            bookingHistoryRepository: BookingHistoryRepository,
            userReference: EntityReference[UserId],
            reactiveMongoApi: ReactiveMongoApi) =
    Props(classOf[UserTimeBookingAggregateMock],
          systemServices,
          clientReceiver,
          bookingHistoryRepository,
          userReference,
          reactiveMongoApi)
}

class UserTimeBookingAggregateMock(
    systemServices: SystemServices,
    clientReceiver: ClientReceiver,
    bookingHistoryRepository: BookingHistoryRepository,
    userReference: EntityReference[UserId],
    reactiveMongoApi: ReactiveMongoApi)
    extends UserTimeBookingAggregate(systemServices,
                                     clientReceiver,
                                     bookingHistoryRepository,
                                     userReference,
                                     reactiveMongoApi)
