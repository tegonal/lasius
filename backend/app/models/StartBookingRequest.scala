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

import domain.UserTimeBookingAggregate.StartBookingCommand
import models.OrganisationId.OrganisationReference
import models.ProjectId.ProjectReference
import org.joda.time.DateTime
import play.api.libs.json.{Json, OFormat}
import BaseFormat._

case class StartBookingRequest(projectId: ProjectId,
                               tags: Set[Tag],
                               start: Option[DateTime]) {
  def toCommand(organisationReference: OrganisationReference,
                projectReference: ProjectReference)(implicit
      subject: Subject): StartBookingCommand =
    StartBookingCommand(subject.userReference,
                        organisationReference,
                        projectReference,
                        tags,
                        start.getOrElse(DateTime.now()))
}

object StartBookingRequest {
  implicit val startBookingFormat: OFormat[StartBookingRequest] =
    Json.format[StartBookingRequest]
}
