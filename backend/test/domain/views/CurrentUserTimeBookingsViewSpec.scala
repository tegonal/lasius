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
import akka.actor.ActorRef
import akka.pattern.StatusReply.Ack
import akka.testkit._
import domain.AggregateRoot.InitializeViewLive
import domain.views.CurrentUserTimeBookingsView.{
  CurrentTimeBookings,
  InitializeCurrentTimeBooking
}
import models.LocalDateTimeWithTimeZone.DateTimeHelper
import models._
import org.joda.time.{DateTime, Duration, LocalDate}
import org.mockito.ArgumentMatchers
import org.specs2.mock._
import play.api.test.PlaySpecification

class CurrentUserTimeBookingsViewSpec
    extends PlaySpecification
    with Mockito
    with PersistentActorTestScope {

  "CurrentUserTimeBookingsView UserTimeBookingStarted" should {
    "booking to current state" in new WithCurrentUserTimeBookingsView {
      val day: LocalDate  = LocalDate.now()
      val start: DateTime = DateTime.now().minusHours(2)

      val tag1: SimpleTag = SimpleTag(TagId("tag1"))
      val tag2: SimpleTag = SimpleTag(TagId("tag2"))

      val bookingStartedEvent: UserTimeBookingStartedV3 =
        UserTimeBookingStartedV3(
          id = BookingId(),
          start = start.toLocalDateTimeWithZone,
          userReference = userReference,
          organisationReference = orgReference,
          projectReference = projectReference,
          tags = Set(tag1, tag2)
        )

      probe.send(actorRef, InitializeViewLive(userReference, 0))
      probe.expectMsg(JournalReadingViewIsLive)

      val state: CurrentUserTimeBookingEvent = CurrentUserTimeBookingEvent(
        CurrentUserTimeBooking(userReference,
                               day,
                               Some(bookingStartedEvent),
                               None,
                               Duration.ZERO))

      probe.send(actorRef, bookingStartedEvent)
      probe.expectMsg(Ack)

      there.was(
        one(clientReceiver) ! (ArgumentMatchers.eq(
          userReference.id), ArgumentMatchers.eq(state), ArgumentMatchers.eq(
          List(userReference.id))))
    }
  }

  "CurrentUserTimeBookingsView UserTimeBookingStartTimeChanged" should {
    "adjust time of current booking" in new WithCurrentUserTimeBookingsView {
      probe.send(actorRef, InitializeViewLive(userReference, 0))
      probe.expectMsg(JournalReadingViewIsLive)

      val day: LocalDate     = LocalDate.now()
      val start: DateTime    = DateTime.now().minusHours(2)
      val tag1: SimpleTag    = SimpleTag(TagId("tag1"))
      val tag2: SimpleTag    = SimpleTag(TagId("tag2"))
      val newStart: DateTime = start.minusHours(2)

      val booking: UserTimeBookingStartedV3 = UserTimeBookingStartedV3(
        id = BookingId(),
        start = start.toLocalDateTimeWithZone,
        userReference = userReference,
        organisationReference = orgReference,
        projectReference = projectReference,
        tags = Set(tag1, tag2)
      )

      // first start booking
      probe.send(actorRef, booking)
      probe.expectMsg(Ack)

      // then move start time
      probe.send(actorRef,
                 UserTimeBookingStartTimeChanged(booking.id, start, newStart))
      probe.expectMsg(Ack)

      val newBooking: UserTimeBookingStartedV3 =
        booking.copy(start = newStart.toLocalDateTimeWithZone)
      val state: CurrentUserTimeBookingEvent = CurrentUserTimeBookingEvent(
        CurrentUserTimeBooking(userReference,
                               day,
                               Some(newBooking),
                               None,
                               Duration.ZERO))
      there.was(
        one(clientReceiver) ! (ArgumentMatchers.eq(
          userReference.id), ArgumentMatchers.eq(state), ArgumentMatchers.eq(
          List(userReference.id))))
    }
  }

  "CurrentUserTimeBookingsView UserTimeBookingEdited" should {
    "update currently active booking when edited" in new WithCurrentUserTimeBookingsView {
      val project2Reference: EntityReference[ProjectId] =
        EntityReference(ProjectId(), "proj2")

      probe.send(actorRef, InitializeViewLive(userReference, 0))
      probe.expectMsg(JournalReadingViewIsLive)

      val day: LocalDate  = LocalDate.now()
      val end: DateTime   = DateTime.now()
      val start: DateTime = end.minusHours(2)
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

      // first start booking
      probe.send(actorRef, booking)
      probe.expectMsg(Ack)

      // then move start time
      val editedBooking: UserTimeBookingStartedV3 =
        booking.copy(projectReference = Some(project2Reference))
      probe.send(actorRef,
                 UserTimeBookingInProgressEdited(booking, editedBooking))
      probe.expectMsg(Ack)

      val state: CurrentUserTimeBookingEvent = CurrentUserTimeBookingEvent(
        CurrentUserTimeBooking(userReference,
                               day,
                               Some(editedBooking),
                               None,
                               Duration.ZERO))
      there.was(
        one(clientReceiver) ! (ArgumentMatchers.eq(
          userReference.id), ArgumentMatchers.eq(state), ArgumentMatchers.eq(
          List(userReference.id))))
    }

    "adjusted daily total of booking when editing booking" in new WithCurrentUserTimeBookingsView {
      probe.send(actorRef, InitializeViewLive(userReference, 0))
      probe.expectMsg(JournalReadingViewIsLive)

      val day: LocalDate     = LocalDate.now()
      val end: DateTime      = DateTime.now()
      val start: DateTime    = end.minusHours(2)
      val tag1: SimpleTag    = SimpleTag(TagId("tag1"))
      val tag2: SimpleTag    = SimpleTag(TagId("tag2"))
      val duration: Duration = Duration.standardHours(2)

      val booking: BookingV3 = BookingV3(
        id = BookingId(),
        start = start.toLocalDateTimeWithZone,
        end = Some(end.toLocalDateTimeWithZone),
        duration = new Duration(start, end),
        userReference = userReference,
        organisationReference = orgReference,
        projectReference = projectReference,
        tags = Set(tag1, tag2)
      )

      val state: CurrentUserTimeBookingEvent = CurrentUserTimeBookingEvent(
        CurrentUserTimeBooking(userReference, day, None, None, duration))

      probe.send(actorRef, UserTimeBookingStoppedV3(booking))
      probe.expectMsg(Ack)

      there.was(
        one(clientReceiver) ! (ArgumentMatchers.eq(
          userReference.id), ArgumentMatchers.eq(state), ArgumentMatchers.eq(
          List(userReference.id))))

      // edit time booking
      val newDuration: Duration = Duration.standardHours(4)
      // expect new duration of current booking, without booking in progress
      val newState: CurrentUserTimeBookingEvent = CurrentUserTimeBookingEvent(
        CurrentUserTimeBooking(userReference, day, None, None, newDuration))

      val newStart: DateTime = start.minusHours(2)
      probe.send(actorRef,
                 UserTimeBookingEditedV4(
                   booking,
                   booking.copy(start = newStart.toLocalDateTimeWithZone,
                                end = Some(end.toLocalDateTimeWithZone))))
      probe.expectMsg(Ack)

      there.was(
        one(clientReceiver) ! (ArgumentMatchers.eq(
          userReference.id), ArgumentMatchers.eq(newState), ArgumentMatchers.eq(
          List(userReference.id))))
    }
  }

  "CurrentUserTimeBookingsView UserTimeBookingRemoved" should {
    "adjusted daily total of booking when stopped booking in same day was deleted" in new WithCurrentUserTimeBookingsView {
      val day: LocalDate     = LocalDate.now()
      val end: DateTime      = DateTime.now()
      val start: DateTime    = end.minusHours(2)
      val tag1: SimpleTag    = SimpleTag(TagId("tag1"))
      val tag2: SimpleTag    = SimpleTag(TagId("tag2"))
      val duration: Duration = Duration.standardHours(2)

      val booking: UserTimeBookingStartedV3 = UserTimeBookingStartedV3(
        id = BookingId(),
        start = start.toLocalDateTimeWithZone,
        userReference = userReference,
        organisationReference = orgReference,
        projectReference = projectReference,
        tags = Set(tag1, tag2)
      )
      val booking2: BookingV3 = BookingV3(
        id = BookingId(),
        start = start.toLocalDateTimeWithZone,
        end = Some(end.toLocalDateTimeWithZone),
        duration = new Duration(start, end),
        userReference = userReference,
        organisationReference = orgReference,
        projectReference = EntityReference(ProjectId(), "project2"),
        tags = Set(tag1, tag2)
      )

      // initialize
      probe.send(
        actorRef,
        InitializeCurrentTimeBooking(state =
          CurrentTimeBookings(booking = Some(booking),
                              currentDay = day,
                              dailyBookingsMap =
                                Map(booking2.stub -> duration)))
      )
      probe.expectMsg(JournalReadingViewIsLive)

      // remove stopped booking
      probe.send(actorRef, UserTimeBookingRemovedV3(booking2))
      probe.expectMsg(Ack)

      // validate new state
      val newState: CurrentUserTimeBookingEvent = CurrentUserTimeBookingEvent(
        CurrentUserTimeBooking(userReference,
                               day,
                               Some(booking),
                               None,
                               Duration.ZERO))

      there.was(
        one(clientReceiver) ! (ArgumentMatchers.eq(
          userReference.id), ArgumentMatchers.eq(newState), ArgumentMatchers.eq(
          List(userReference.id))))
    }
  }

  trait WithCurrentUserTimeBookingsView extends WithPersistentActorTestScope {
    val probe: TestProbe               = TestProbe()
    val clientReceiver: ClientReceiver = mock[ClientReceiver]

    val userReference: EntityReference[UserId] =
      EntityReference(UserId(), "noob")
    val projectReference: EntityReference[ProjectId] =
      EntityReference(ProjectId(), "proj")
    val orgReference: EntityReference[OrganisationId] =
      EntityReference(OrganisationId(), "team")
    val actorRef: ActorRef = system.actorOf(
      CurrentUserTimeBookingsView.props(clientReceiver, userReference))
  }
}
