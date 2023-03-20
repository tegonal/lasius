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

package domain.views

import actors.ClientReceiver
import akka.PersistentActorTestScope
import akka.actor.Props
import akka.pattern.StatusReply.Ack
import akka.testkit._
import core.{DBSession, DBSupportMock, MockServices, TestDBSupport}
import domain.views.CurrentOrganisationTimeBookingsView.{
  GetCurrentOrganisationTimeBookings,
  Initialize,
  NoResultFound
}
import models.LocalDateTimeWithTimeZone.DateTimeHelper
import models._
import org.joda.time.{DateTime, Duration, LocalDate}
import org.specs2.mock._
import org.specs2.mutable._
import play.modules.reactivemongo.ReactiveMongoApi
import repositories.UserRepository

import scala.concurrent.Future

class CurrentOrganisationTimeBookingsViewSpec
    extends Specification
    with Mockito {

  "CurrentOrganisationTimeBookingsViewSpec" should {
    "correct result for: booking in first organisaton, book in second organisation stop booking" in new PersistentActorTestScope {
      val systemServices = new MockServices(system)
      val probe          = TestProbe()
      val clientReceiver = mock[ClientReceiver]

      val userReference =
        EntityReference(UserId(), "noob")
      val orgReference =
        EntityReference(OrganisationId(), "org")
      val projectReference =
        EntityReference(ProjectId(), "proj")

      val userRepository = mock[UserRepository]

      val actorRef = system.actorOf(
        CurrentOrganisationTimeBookingsViewMock.props(
          userRepository,
          clientReceiver,
          systemServices.reactiveMongoApi,
          systemServices.supportTransaction))

      val day   = LocalDate.now()
      val start = DateTime.now().minusHours(2)

      val tag1 = SimpleTag(TagId("tag1"))
      val tag2 = SimpleTag(TagId("tag2"))

      val booking = BookingV2(BookingId(),
                              start.toLocalDateTimeWithZone(),
                              None,
                              userReference,
                              orgReference,
                              projectReference,
                              Set(tag1, tag2))

      val state1 = CurrentUserTimeBooking(userReference,
                                          day,
                                          Some(booking),
                                          None,
                                          Duration.ZERO)

      probe.send(actorRef, CurrentUserTimeBookingEvent(state1))
      probe.expectMsg(Ack)

      probe.send(actorRef, GetCurrentOrganisationTimeBookings(orgReference.id))
      probe.expectMsgPF() {
        case CurrentOrganisationTimeBookings(orgReference.id, Seq(`state1`)) =>
          true
      }

      // start booking in second organisation
      val org2Reference =
        EntityReference(OrganisationId(), "org2")
      val project2Reference =
        EntityReference(ProjectId(), "proj2")

      val booking2 = BookingV2(BookingId(),
                               start.toLocalDateTimeWithZone(),
                               None,
                               userReference,
                               org2Reference,
                               project2Reference,
                               Set())
      val newState1 = state1.copy(booking = None)
      val state2 = CurrentUserTimeBooking(userReference,
                                          day,
                                          Some(booking2),
                                          None,
                                          Duration.ZERO)

      probe.send(actorRef, CurrentUserTimeBookingEvent(state2))
      probe.expectMsg(Ack)

      probe.send(actorRef, GetCurrentOrganisationTimeBookings(orgReference.id))
      probe.expectMsgPF() {
        case CurrentOrganisationTimeBookings(orgReference.id,
                                             Seq(`newState1`)) =>
          true
      }

      probe.send(actorRef, GetCurrentOrganisationTimeBookings(org2Reference.id))
      probe.expectMsgPF() {
        case CurrentOrganisationTimeBookings(org2Reference.id, Seq(`state2`)) =>
          true
      }

      // stop booking in second organisation
      val newState2 = state2.copy(booking = None)
      probe.send(actorRef, CurrentUserTimeBookingEvent(newState2))
      probe.expectMsg(Ack)

      probe.send(actorRef, GetCurrentOrganisationTimeBookings(org2Reference.id))
      probe.expectMsgPF() {
        case CurrentOrganisationTimeBookings(org2Reference.id,
                                             Seq(`newState2`)) =>
          true
      }
    }
  }

  "initialize organisation time bookings with active users only" in new PersistentActorTestScope {
    val systemServices = new MockServices(system)
    val probe          = TestProbe()
    val clientReceiver = mock[ClientReceiver]

    val userRepository = mock[UserRepository]

    val actorRef = system.actorOf(
      CurrentOrganisationTimeBookingsViewMock.props(
        userRepository,
        clientReceiver,
        systemServices.reactiveMongoApi,
        systemServices.supportTransaction))

    val organisationReference = EntityReference(OrganisationId(), "org")

    val user1 = User(
      id = UserId(),
      key = "user1",
      email = "test@user.com",
      password = "",
      firstName = "",
      lastName = "",
      active = true,
      role = FreeUser,
      organisations = Seq(
        UserOrganisation(
          organisationReference = organisationReference,
          `private` = false,
          role = OrganisationMember,
          plannedWorkingHours = WorkingHours(),
          projects = Seq()
        )
      ),
      settings = None
    )

    val user2 = User(
      id = UserId(),
      key = "user2",
      email = "test@user2.com",
      password = "",
      firstName = "",
      lastName = "",
      active = false,
      role = FreeUser,
      organisations = Seq(
        UserOrganisation(
          organisationReference = organisationReference,
          `private` = false,
          role = OrganisationMember,
          plannedWorkingHours = WorkingHours(),
          projects = Seq()
        )
      ),
      settings = None
    )

    userRepository
      .findAll()(any[DBSession])
      .returns(Future.successful(Seq(user1, user2)))

    probe.send(actorRef, Initialize)
    probe.expectMsg(Ack)

    probe.send(actorRef,
               GetCurrentOrganisationTimeBookings(organisationReference.id))
    val today = LocalDate.now()
    val expectedResult = CurrentOrganisationTimeBookings(
      organisationReference.id,
      Seq(
        CurrentUserTimeBooking(user1.getReference(),
                               today,
                               None,
                               None,
                               Duration.ZERO)))
    probe.expectMsgPF() {
      case `expectedResult` =>
        true
      case r => failure(s"expected: $expectedResult, \nreceived: $r")
    }
  }

  "initialize without users" in new PersistentActorTestScope {
    val systemServices = new MockServices(system)
    val probe          = TestProbe()
    val clientReceiver = mock[ClientReceiver]

    val userRepository = mock[UserRepository]

    val actorRef = system.actorOf(
      CurrentOrganisationTimeBookingsViewMock.props(
        userRepository,
        clientReceiver,
        systemServices.reactiveMongoApi,
        systemServices.supportTransaction))

    val organisationReference = EntityReference(OrganisationId(), "org")

    userRepository
      .findAll()(any[DBSession])
      .returns(Future.successful(Seq()))

    probe.send(actorRef, Initialize)
    probe.expectMsg(Ack)

    probe.send(actorRef,
               GetCurrentOrganisationTimeBookings(organisationReference.id))
    probe.expectMsg(NoResultFound)
  }
}

class CurrentOrganisationTimeBookingsViewMock(
    userRepository: UserRepository,
    clientReceiver: ClientReceiver,
    reactiveMongoApi: ReactiveMongoApi,
    supportTransaction: Boolean)
    extends CurrentOrganisationTimeBookingsView(userRepository,
                                                clientReceiver,
                                                reactiveMongoApi,
                                                supportTransaction)
    with DBSupportMock {}

object CurrentOrganisationTimeBookingsViewMock {
  def props(userRepository: UserRepository,
            clientReceiver: ClientReceiver,
            reactiveMongoApi: ReactiveMongoApi,
            supportTransaction: Boolean): Props =
    Props(classOf[CurrentOrganisationTimeBookingsViewMock],
          userRepository,
          clientReceiver,
          reactiveMongoApi,
          supportTransaction)
}
