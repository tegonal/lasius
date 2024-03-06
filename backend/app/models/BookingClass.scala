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
import play.api.libs.json._

sealed trait BookingClass {
  val bookingCategories: Set[Tag]
}

case class Project(id: ProjectId,
                   key: String,
                   organisationReference: OrganisationReference,
                   bookingCategories: Set[Tag],
                   active: Boolean,
                   createdBy: UserReference,
                   deactivatedBy: Option[UserReference])
    extends BaseEntityWithOrgRelation[ProjectId]
    with BookingClass {
  def reference: ProjectReference = EntityReference(id, key)
}

object Project {
  implicit val format: Format[Project] = Json.format[Project]
}

case class AbsenceSettings(bookingCategories: Set[Tag]) extends BookingClass

object AbsenceSettings {
  implicit val format: Format[AbsenceSettings] =
    Json.format[AbsenceSettings]
}
