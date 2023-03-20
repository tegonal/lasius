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
import akka.actor._
import akka.pattern.StatusReply
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
import org.specs2.mock._
import org.specs2.mutable._
import reactivemongo.api.bson.BSONObjectID

class CurrentUserTimeBookingsViewSpec extends Specification with Mockito {

  "CurrentUserTimeBookingsView UserTimeBookingStarted" should {
    "booking to current state" in new PersistentActorTestScope {

      val probe          = TestProbe()
      val clientReceiver = mock[ClientReceiver]

      val userReference =
        EntityReference(UserId(), "noob")
      val projectReference =
        EntityReference(ProjectId(), "proj")
      val orgReference =
        EntityReference(OrganisationId(), "team")
      val actorRef = system.actorOf(
        CurrentUserTimeBookingsViewMock.props(userReference, clientReceiver))

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

      probe.send(actorRef, InitializeViewLive(userReference, 0))
      probe.expectMsg(JournalReadingViewIsLive)

      val state = CurrentUserTimeBookingEvent(
        CurrentUserTimeBooking(userReference,
                               day,
                               Some(booking),
                               None,
                               Duration.ZERO))

      probe.send(actorRef, UserTimeBookingStartedV2(booking))
      probe.expectMsg(Ack)

      there.was(
        one(clientReceiver) ! (org.mockito.ArgumentMatchers
          .eq(userReference.id), org.mockito.ArgumentMatchers
          .eq(state), org.mockito.ArgumentMatchers.eq(List(userReference.id))))
    }
  }

  "CurrentUserTimeBookingsView UserTimeBookingStartTimeChanged" should {
    "adjust time of current booking" in new PersistentActorTestScope {

      val userReference =
        EntityReference(UserId(), "noob")
      val projectReference =
        EntityReference(ProjectId(), "proj")
      val teamReference =
        EntityReference(OrganisationId(), "team")
      val probe          = TestProbe()
      val clientReceiver = mock[ClientReceiver]
      val actorRef = system.actorOf(
        CurrentUserTimeBookingsViewMock.props(userReference, clientReceiver))

      probe.send(actorRef, InitializeViewLive(userReference, 0))
      probe.expectMsg(JournalReadingViewIsLive)

      val day      = LocalDate.now()
      val start    = DateTime.now().minusHours(2)
      val tag1     = SimpleTag(TagId("tag1"))
      val tag2     = SimpleTag(TagId("tag2"))
      val newStart = start.minusHours(2)

      val booking = BookingV2(BookingId(),
                              start.toLocalDateTimeWithZone(),
                              None,
                              userReference,
                              teamReference,
                              projectReference,
                              Set(tag1, tag2))

      // first start booking
      probe.send(actorRef, UserTimeBookingStartedV2(booking))
      probe.expectMsg(Ack)

      // then move start time
      probe.send(actorRef,
                 UserTimeBookingStartTimeChanged(booking.id, start, newStart))
      probe.expectMsg(Ack)

      val newBooking = booking.copy(start = newStart.toLocalDateTimeWithZone())
      val state = CurrentUserTimeBookingEvent(
        CurrentUserTimeBooking(userReference,
                               day,
                               Some(newBooking),
                               None,
                               Duration.ZERO))
      there.was(
        one(clientReceiver) ! (org.mockito.ArgumentMatchers
          .eq(userReference.id), org.mockito.ArgumentMatchers
          .eq(state), org.mockito.ArgumentMatchers.eq(List(userReference.id))))
    }
  }

  "CurrentUserTimeBookingsView UserTimeBookingEdited" should {
    "update currently active booking when edited" in new PersistentActorTestScope {

      val userReference =
        EntityReference(UserId(), "noob")
      val projectReference =
        EntityReference(ProjectId(), "proj")
      val project2Reference =
        EntityReference(ProjectId(), "proj2")
      val teamReference =
        EntityReference(OrganisationId(), "team")
      val probe          = TestProbe()
      val clientReceiver = mock[ClientReceiver]
      val actorRef = system.actorOf(
        CurrentUserTimeBookingsViewMock.props(userReference, clientReceiver))

      probe.send(actorRef, InitializeViewLive(userReference, 0))
      probe.expectMsg(JournalReadingViewIsLive)

      val day   = LocalDate.now()
      val end   = DateTime.now()
      val start = end.minusHours(2)
      val tag1  = SimpleTag(TagId("tag1"))
      val tag2  = SimpleTag(TagId("tag2"))

      val booking = BookingV2(BookingId(),
                              start.toLocalDateTimeWithZone(),
                              None,
                              userReference,
                              teamReference,
                              projectReference,
                              Set(tag1, tag2))

      // first start booking
      probe.send(actorRef, UserTimeBookingStartedV2(booking))
      probe.expectMsg(Ack)

      // then move start time
      val editedBooking = booking.copy(projectReference = project2Reference)
      probe.send(actorRef, UserTimeBookingEditedV3(booking, editedBooking))
      probe.expectMsg(Ack)

      val state = CurrentUserTimeBookingEvent(
        CurrentUserTimeBooking(userReference,
                               day,
                               Some(editedBooking),
                               None,
                               Duration.ZERO))
      there.was(
        one(clientReceiver) ! (org.mockito.ArgumentMatchers
          .eq(userReference.id), org.mockito.ArgumentMatchers
          .eq(state), org.mockito.ArgumentMatchers.eq(List(userReference.id))))
    }

    "adjusted daily total of booking when editing booking" in new PersistentActorTestScope {

      val userReference =
        EntityReference(UserId(), "noob")
      val projectReference =
        EntityReference(ProjectId(), "proj")
      val teamReference =
        EntityReference(OrganisationId(), "team")
      val probe          = TestProbe()
      val clientReceiver = mock[ClientReceiver]
      val actorRef = system.actorOf(
        CurrentUserTimeBookingsViewMock.props(userReference, clientReceiver))

      probe.send(actorRef, InitializeViewLive(userReference, 0))
      probe.expectMsg(JournalReadingViewIsLive)

      val day      = LocalDate.now()
      val end      = DateTime.now()
      val start    = end.minusHours(2)
      val tag1     = SimpleTag(TagId("tag1"))
      val tag2     = SimpleTag(TagId("tag2"))
      val duration = Duration.standardHours(2)

      val booking = BookingV2(BookingId(),
                              start.toLocalDateTimeWithZone(),
                              Some(end.toLocalDateTimeWithZone()),
                              userReference,
                              teamReference,
                              projectReference,
                              Set(tag1, tag2))

      val state = CurrentUserTimeBookingEvent(
        CurrentUserTimeBooking(userReference, day, None, None, duration))

      probe.send(actorRef, UserTimeBookingStoppedV2(booking))
      probe.expectMsg(Ack)

      there.was(
        one(clientReceiver) ! (org.mockito.ArgumentMatchers
          .eq(userReference.id), org.mockito.ArgumentMatchers
          .eq(state), org.mockito.ArgumentMatchers.eq(List(userReference.id))))

      // edit time booking
      val newDuration = Duration.standardHours(4)
      // expect new duration of current booking, without booking in progress
      val newState = CurrentUserTimeBookingEvent(
        CurrentUserTimeBooking(userReference, day, None, None, newDuration))

      val newStart = start.minusHours(2)
      probe.send(actorRef,
                 UserTimeBookingEditedV3(
                   booking,
                   booking.copy(start = newStart.toLocalDateTimeWithZone(),
                                end = Some(end.toLocalDateTimeWithZone()))))
      probe.expectMsg(Ack)

      there.was(one(clientReceiver) ! (org.mockito.ArgumentMatchers
        .eq(userReference.id), org.mockito.ArgumentMatchers
        .eq(newState), org.mockito.ArgumentMatchers.eq(List(userReference.id))))
    }
  }

  "CurrentUserTimeBookingsView UserTimeBookingRemoved" should {
    "adjusted daily total of booking when stopped booking in same day was deleted" in new PersistentActorTestScope {

      val userReference =
        EntityReference(UserId(), "noob")
      val projectReference =
        EntityReference(ProjectId(), "proj")
      val teamReference =
        EntityReference(OrganisationId(), "team")
      val probe          = TestProbe()
      val clientReceiver = mock[ClientReceiver]
      val actorRef = system.actorOf(
        CurrentUserTimeBookingsViewMock.props(userReference, clientReceiver))

      val day      = LocalDate.now()
      val end      = DateTime.now()
      val start    = end.minusHours(2)
      val tag1     = SimpleTag(TagId("tag1"))
      val tag2     = SimpleTag(TagId("tag2"))
      val duration = Duration.standardHours(2)

      val booking = BookingV2(BookingId(),
                              start.toLocalDateTimeWithZone(),
                              None,
                              userReference,
                              teamReference,
                              projectReference,
                              Set(tag1, tag2))
      val booking2 = BookingV2(
        BookingId(),
        start.toLocalDateTimeWithZone(),
        Some(end.toLocalDateTimeWithZone()),
        userReference,
        teamReference,
        EntityReference(ProjectId(), "project2"),
        Set(tag1, tag2)
      )

      // initialize
      probe.send(
        actorRef,
        InitializeCurrentTimeBooking(state =
          CurrentTimeBookings(booking = Some(booking),
                              currentDay = day,
                              dailyBookingsMap =
                                Map(booking2.createStub -> duration)))
      )
      probe.expectMsg(JournalReadingViewIsLive)

      // remove stopped booking
      probe.send(actorRef, UserTimeBookingRemovedV2(booking2))
      probe.expectMsg(Ack)

      // validate new state
      val newState = CurrentUserTimeBookingEvent(
        CurrentUserTimeBooking(userReference,
                               day,
                               Some(booking),
                               None,
                               Duration.ZERO))

      there.was(one(clientReceiver) ! (org.mockito.ArgumentMatchers
        .eq(userReference.id), org.mockito.ArgumentMatchers
        .eq(newState), org.mockito.ArgumentMatchers.eq(List(userReference.id))))
    }

    "adjusted daily total of booking when current booking in same day was deleted" in new PersistentActorTestScope {

      val userReference =
        EntityReference(UserId(), "noob")
      val projectReference =
        EntityReference(ProjectId(), "proj")
      val teamReference =
        EntityReference(OrganisationId(), "team")
      val probe          = TestProbe()
      val clientReceiver = mock[ClientReceiver]
      val actorRef = system.actorOf(
        CurrentUserTimeBookingsViewMock.props(userReference, clientReceiver))

      val day      = LocalDate.now()
      val end      = DateTime.now()
      val start    = end.minusHours(2)
      val tag1     = SimpleTag(TagId("tag1"))
      val tag2     = SimpleTag(TagId("tag2"))
      val duration = Duration.standardHours(2)

      val booking = BookingV2(BookingId(),
                              start.toLocalDateTimeWithZone(),
                              None,
                              userReference,
                              teamReference,
                              projectReference,
                              Set(tag1, tag2))
      val booking2 = BookingV2(
        BookingId(),
        start.toLocalDateTimeWithZone(),
        Some(end.toLocalDateTimeWithZone()),
        userReference,
        teamReference,
        EntityReference(ProjectId(), "project2"),
        Set(tag1, tag2)
      )

      // initialize
      probe.send(
        actorRef,
        InitializeCurrentTimeBooking(state =
          CurrentTimeBookings(booking = Some(booking),
                              currentDay = day,
                              dailyBookingsMap =
                                Map(booking2.createStub -> duration)))
      )
      probe.expectMsg(JournalReadingViewIsLive)

      // remove current booking
      probe.send(actorRef, UserTimeBookingRemovedV2(booking))
      probe.expectMsg(Ack)

      // validate new state
      val newState = CurrentUserTimeBookingEvent(
        CurrentUserTimeBooking(userReference, day, None, None, duration))

      there.was(one(clientReceiver) ! (org.mockito.ArgumentMatchers
        .eq(userReference.id), org.mockito.ArgumentMatchers
        .eq(newState), org.mockito.ArgumentMatchers.eq(List(userReference.id))))
    }
  }
}

object CurrentUserTimeBookingsViewMock {
  def props(userReference: EntityReference[UserId],
            clientReceiver: ClientReceiver) =
    Props(classOf[CurrentUserTimeBookingsView], clientReceiver, userReference)
}
