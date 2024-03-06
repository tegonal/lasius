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

import models.BaseFormat._
import models.OrganisationId.OrganisationReference
import models.ProjectId.ProjectReference
import models.UserId.UserReference
import org.joda.time.LocalDate
import play.api.libs.json.{Format, Json}

sealed trait BookingType

sealed trait WorkingTimeBooking extends BookingType

case object ProjectBooking extends WorkingTimeBooking

sealed trait NonWorkingTimeBooking extends BookingType
case object HolidayBooking         extends NonWorkingTimeBooking
case object PublicHolidayBooking   extends NonWorkingTimeBooking
case object AbsenceBooking         extends NonWorkingTimeBooking

object BookingType {
  implicit val format: Format[BookingType] = enumFormat[BookingType]

  val values =
    Seq(ProjectBooking, HolidayBooking, PublicHolidayBooking, AbsenceBooking)
}

object NonWorkingTimeBooking {
  implicit val format: Format[NonWorkingTimeBooking] =
    enumFormat[NonWorkingTimeBooking]
}

case class BookingV3(id: BookingId,
                     bookingType: BookingType,
                     start: LocalDateTimeWithTimeZone,
                     end: Option[LocalDateTimeWithTimeZone],
                     duration: org.joda.time.Duration,
                     userReference: UserReference,
                     organisationReference: OrganisationReference,
                     projectReference: Option[ProjectReference],
                     tags: Set[Tag],
                     bookingHash: Long)
    extends BaseEntityWithOrgRelation[BookingId] {

  val stub: BookingStub =
    BookingStub(bookingType, projectReference, tags, bookingHash)

  val day: LocalDate = end.getOrElse(start).toDateTime.toLocalDate
}

object BookingV3 {

  def apply(id: BookingId,
            bookingType: BookingType,
            start: LocalDateTimeWithTimeZone,
            end: Option[LocalDateTimeWithTimeZone],
            duration: org.joda.time.Duration,
            userReference: UserReference,
            organisationReference: OrganisationReference,
            projectReference: Option[ProjectReference],
            tags: Set[Tag]): BookingV3 =
    BookingV3(
      id = id,
      bookingType = bookingType,
      start = start,
      end = end,
      duration = duration,
      userReference = userReference,
      organisationReference = organisationReference,
      projectReference = projectReference,
      tags = tags,
      bookingHash = BookingHash.createHash(projectReference, tags)
    )

  def apply(id: BookingId,
            start: LocalDateTimeWithTimeZone,
            end: Option[LocalDateTimeWithTimeZone],
            duration: org.joda.time.Duration,
            userReference: UserReference,
            organisationReference: OrganisationReference,
            projectReference: ProjectReference,
            tags: Set[Tag]): BookingV3 =
    BookingV3(
      id = id,
      bookingType = ProjectBooking,
      start = start,
      end = end,
      duration = duration,
      userReference = userReference,
      organisationReference = organisationReference,
      projectReference = Some(projectReference),
      tags = tags,
      bookingHash = BookingHash.createHash(Some(projectReference), tags)
    )

  implicit val bookingFormat: Format[BookingV3] =
    Json.using[Json.WithDefaultValues].format[BookingV3]
}
