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
import org.joda.time.{DateTime, Duration, LocalDate, LocalDateTime}
import play.api.libs.json.{Json, OFormat}
import models.BaseFormat._

case class BookingByProject(_id: BookingByProjectId,
                            userReference: UserReference,
                            organisationReference: OrganisationReference,
                            day: LocalDate,
                            projectReference: ProjectReference,
                            duration: Duration)
    extends OperatorEntity[BookingByProjectId, BookingByProject] {
  val id: BookingByProjectId = _id

  def invert: BookingByProject = {
    BookingByProject(id,
                     userReference,
                     organisationReference,
                     day,
                     projectReference,
                     Duration.ZERO.minus(duration))
  }

  def duration(duration: Duration): BookingByProject = {
    copy(duration = duration)
  }
}

object BookingByProject {
  implicit val bookingByProjectFormat: OFormat[BookingByProject] =
    Json.format[BookingByProject]
}
