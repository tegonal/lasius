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

import models.OrganisationId.OrganisationReference
import models.ProjectId.ProjectReference
import models.UserId.UserReference
import org.joda.time.Duration
import play.api.libs.json.{Format, Json}

import scala.annotation.nowarn

@deprecated("Don't use events based on Booking V2", "LasiusV1.2")
case class BookingV2(id: BookingId,
                     start: LocalDateTimeWithTimeZone,
                     end: Option[LocalDateTimeWithTimeZone],
                     userReference: UserReference,
                     organisationReference: OrganisationReference,
                     projectReference: ProjectReference,
                     tags: Set[Tag],
                     bookingHash: Long)
    extends BaseEntity[BookingId] {

  /** Migration to V3 by:
    *   - map projectReference to optional type
    *   - provide empty duration
    *   - map to default type 'project'
    * @return
    */
  def toV3: BookingV3 = {
    BookingV3(
      id = id,
      bookingType = ProjectBooking,
      start = start,
      end = end,
      duration = end.fold(new Duration(0))(e =>
        new Duration(start.toDateTime, e.toDateTime)),
      userReference = userReference,
      organisationReference = organisationReference,
      projectReference = Some(projectReference),
      tags = tags,
      bookingHash = bookingHash
    )
  }
}

object BookingV2 {

  // noinspection ScalaDeprecation
  @nowarn("cat=deprecation")
  def apply(id: BookingId,
            start: LocalDateTimeWithTimeZone,
            end: Option[LocalDateTimeWithTimeZone],
            userReference: UserReference,
            organisationReference: OrganisationReference,
            projectReference: ProjectReference,
            tags: Set[Tag]): BookingV2 =
    BookingV2(
      id = id,
      start = start,
      end = end,
      userReference = userReference,
      organisationReference = organisationReference,
      projectReference = projectReference,
      tags = tags,
      bookingHash = BookingHash.createHash(Some(projectReference), tags)
    )

  // noinspection ScalaDeprecation
  @nowarn("cat=deprecation")
  implicit val bookingFormat: Format[BookingV2] =
    Json.using[Json.WithDefaultValues].format[BookingV2]
}
