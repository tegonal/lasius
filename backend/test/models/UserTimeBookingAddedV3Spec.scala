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
