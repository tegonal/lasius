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
import models.UserId.UserReference
import org.joda.time.{DateTime, Duration, LocalDate, LocalDateTime}
import play.api.libs.json._
import models.BaseFormat._

case class BookingByTag(_id: BookingByTagId,
                        userReference: UserReference,
                        organisationReference: OrganisationReference,
                        day: LocalDate,
                        tagId: TagId,
                        duration: Duration)
    extends OperatorEntity[BookingByTagId, BookingByTag] {
  val id: BookingByTagId = _id

  def duration(duration: Duration): BookingByTag = {
    copy(duration = duration)
  }

  def invert: BookingByTag = {
    BookingByTag(id,
                 userReference,
                 organisationReference,
                 day,
                 tagId,
                 Duration.ZERO.minus(duration))
  }
}

object BookingByTag {
  implicit val bookingByTagFormat: OFormat[BookingByTag] =
    Json.format[BookingByTag]
}
