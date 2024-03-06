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
import org.joda.time.{Duration, LocalDate}
import play.api.libs.json.{Json, OFormat}
import models.BaseFormat._

case class BookingByType(_id: BookingByTypeId,
                         userReference: UserReference,
                         organisationReference: OrganisationReference,
                         day: LocalDate,
                         bookingType: BookingType,
                         duration: Duration)
    extends OperatorEntity[BookingByTypeId, BookingByType] {
  val id: BookingByTypeId = _id

  def invert: BookingByType = {
    BookingByType(id,
                  userReference,
                  organisationReference,
                  day,
                  bookingType,
                  Duration.ZERO.minus(duration))
  }

  def duration(duration: Duration): BookingByType = {
    copy(duration = duration)
  }
}

object BookingByType {
  implicit val bookingByTypeFormat: OFormat[BookingByType] =
    Json.format[BookingByType]
}
