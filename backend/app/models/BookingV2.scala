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
import play.api.libs.json.{Format, Json}

case class BookingV2(id: BookingId,
                     start: LocalDateTimeWithTimeZone,
                     end: Option[LocalDateTimeWithTimeZone],
                     userReference: UserReference,
                     organisationReference: OrganisationReference,
                     projectReference: ProjectReference,
                     tags: Set[Tag],
                     bookingHash: Long)
    extends BaseEntity[BookingId] {

  def createStub: BookingStub = {
    BookingStub(projectReference, tags, bookingHash)
  }
}

object BookingV2 {

  def apply(id: BookingId,
            start: LocalDateTimeWithTimeZone,
            end: Option[LocalDateTimeWithTimeZone],
            userReference: UserReference,
            organisationReference: OrganisationReference,
            projectReference: ProjectReference,
            tags: Set[Tag]): BookingV2 =
    BookingV2(id,
              start,
              end,
              userReference,
              organisationReference,
              projectReference,
              tags,
              BookingHash.createHash(projectReference, tags))

  implicit val bookingFormat: Format[BookingV2] =
    Json.using[Json.WithDefaultValues].format[BookingV2]
}
