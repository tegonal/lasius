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
import akka.actor.{ActorRef, Props}
import akka.pattern.StatusReply.Ack
import akka.testkit._
import core.{DBSession, DBSupportMock, MockServices}
import domain.views.CurrentOrganisationTimeBookingsView.{
  GetCurrentOrganisationTimeBookings,
  Initialize,
  NoResultFound
}
import models.LocalDateTimeWithTimeZone.DateTimeHelper
import models._
import org.joda.time.{DateTime, Duration, LocalDate}
import org.specs2.mock._
import play.api.test.PlaySpecification
import play.modules.reactivemongo.ReactiveMongoApi
import repositories.UserRepository

import scala.concurrent.Future

class CurrentOrganisationTimeBookingsViewSpec
    extends PlaySpecification
    with Mockito
    with PersistentActorTestScope {

  "CurrentOrganisationTimeBookingsViewSpec" should {
    "correct result for: booking in first organisaton, book in second organisation stop booking" in new WithCurrentOrganisationTimeBookingsViewMock {
      val day: LocalDate  = LocalDate.now()
      val start: DateTime = DateTime.now().minusHours(2)

      val tag1: SimpleTag = SimpleTag(TagId("tag1"))
      val tag2: SimpleTag = SimpleTag(TagId("tag2"))

      val booking: UserTimeBookingStartedV3 = UserTimeBookingStartedV3(
        id = BookingId(),
        start = start.toLocalDateTimeWithZone,
        userReference = userReference,
        organisationReference = orgReference,
        projectReference = projectReference,
        tags = Set(tag1, tag2)
      )

      val state1: CurrentUserTimeBooking = CurrentUserTimeBooking(userReference,
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
      val org2Reference: EntityReference[OrganisationId] =
        EntityReference(OrganisationId(), "org2")
      val project2Reference: EntityReference[ProjectId] =
        EntityReference(ProjectId(), "proj2")

      val booking2: UserTimeBookingStartedV3 = UserTimeBookingStartedV3(
        id = BookingId(),
        start = start.toLocalDateTimeWithZone,
        userReference = userReference,
        organisationReference = org2Reference,
        projectReference = project2Reference,
        tags = Set()
      )
      val newState1: CurrentUserTimeBooking = state1.copy(booking = None)
      val state2: CurrentUserTimeBooking =
        CurrentUserTimeBooking(userReference,
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
      val newState2: CurrentUserTimeBooking = state2.copy(booking = None)
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

  "initialize organisation time bookings with active users only" in new WithCurrentOrganisationTimeBookingsViewMock {
    val user1: User = User(
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
          organisationReference = orgReference,
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
          organisationReference = orgReference,
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

    probe.send(actorRef, GetCurrentOrganisationTimeBookings(orgReference.id))
    val today: LocalDate = LocalDate.now()
    val expectedResult: CurrentOrganisationTimeBookings =
      CurrentOrganisationTimeBookings(orgReference.id,
                                      Seq(
                                        CurrentUserTimeBooking(user1.reference,
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

  "initialize without users" in new WithCurrentOrganisationTimeBookingsViewMock {
    userRepository
      .findAll()(any[DBSession])
      .returns(Future.successful(Seq()))

    probe.send(actorRef, Initialize)
    probe.expectMsg(Ack)

    probe.send(actorRef, GetCurrentOrganisationTimeBookings(orgReference.id))
    probe.expectMsg(NoResultFound)
  }

  trait WithCurrentOrganisationTimeBookingsViewMock
      extends WithPersistentActorTestScope {
    val systemServices                 = new MockServices(system)
    val probe: TestProbe               = TestProbe()
    val clientReceiver: ClientReceiver = mock[ClientReceiver]

    val userReference: EntityReference[UserId] =
      EntityReference(UserId(), "noob")
    val orgReference: EntityReference[OrganisationId] =
      EntityReference(OrganisationId(), "org")
    val projectReference: EntityReference[ProjectId] =
      EntityReference(ProjectId(), "proj")

    val userRepository: UserRepository = mock[UserRepository]

    val actorRef: ActorRef = system.actorOf(
      CurrentOrganisationTimeBookingsViewMock.props(
        userRepository,
        clientReceiver,
        systemServices.reactiveMongoApi,
        systemServices.supportTransaction))
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
    Props(
      new CurrentOrganisationTimeBookingsViewMock(userRepository,
                                                  clientReceiver,
                                                  reactiveMongoApi,
                                                  supportTransaction))
}
