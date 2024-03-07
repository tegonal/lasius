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

package models

import models.LocalDateTimeWithTimeZone.DateTimeHelper
import org.joda.time.{DateTime, Duration}
import org.specs2.mutable._

class UserTimeBookingAddedV3Spec extends Specification {
  "Converting UserTimeBookingAddedV3 to a BookingV3" should {
    "calculate duration if end date is provided" in {
      val start = DateTime.now()
      val end   = start.plusHours(3)
      val bookingEvent = UserTimeBookingAddedV3(
        id = BookingId(),
        bookingType = ProjectBooking,
        userReference = EntityReference(UserId(), "user-id"),
        organisationReference = EntityReference(OrganisationId(), "org-id"),
        projectReference = Some(EntityReference(ProjectId(), "project-id")),
        tags = Set(),
        start = start,
        endOrDuration = Left(end)
      )
      val booking = bookingEvent.toBooking

      booking.start should beEqualTo(start.toLocalDateTimeWithZone)
      booking.end should beSome(end.toLocalDateTimeWithZone)
      booking.duration should beEqualTo(new Duration(start, end))
    }
  }

  "use provided duration without end if duration was provided" in {
    val start    = DateTime.now()
    val duration = new Duration(24556)
    val bookingEvent = UserTimeBookingAddedV3(
      id = BookingId(),
      bookingType = ProjectBooking,
      userReference = EntityReference(UserId(), "user-id"),
      organisationReference = EntityReference(OrganisationId(), "org-id"),
      projectReference = Some(EntityReference(ProjectId(), "project-id")),
      tags = Set(),
      start = start,
      endOrDuration = Right(duration)
    )
    val booking = bookingEvent.toBooking

    booking.start should beEqualTo(start.toLocalDateTimeWithZone)
    booking.end should beNone
    booking.duration should beEqualTo(duration)
  }
}
