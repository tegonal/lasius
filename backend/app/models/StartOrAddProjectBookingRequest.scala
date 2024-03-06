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

import core.Validation
import domain.UserTimeBookingAggregate.{
  AddBookingCommand,
  StartProjectBookingCommand,
  UserTimeBookingCommand
}
import models.OrganisationId.OrganisationReference
import models.ProjectId.ProjectReference
import org.joda.time.{DateTime, Duration}
import play.api.libs.json.{Json, OFormat}
import models.BaseFormat._

import scala.concurrent.{ExecutionContext, Future}

/** end date and duration are mutually exclusive
  * @param projectId
  * @param tags
  * @param start
  * @param end
  * @param duration
  */
case class StartOrAddProjectBookingRequest(projectId: ProjectId,
                                           tags: Set[Tag],
                                           start: DateTime,
                                           end: Option[DateTime] = None,
                                           duration: Option[Duration] = None)
    extends Validation {
  def toCommand(organisationReference: OrganisationReference,
                projectReference: ProjectReference)(implicit
      subject: Subject,
      executionContext: ExecutionContext): Future[UserTimeBookingCommand] = {

    validateMutualExclusive("end_date" -> end, "duration" -> duration).map {
      case None =>
        StartProjectBookingCommand(userReference = subject.userReference,
                                   organisationReference =
                                     organisationReference,
                                   projectReference = projectReference,
                                   tags = tags,
                                   start = start)
      case Some(endOrDuration) =>
        AddBookingCommand(
          bookingType = ProjectBooking,
          userReference = subject.userReference,
          organisationReference = organisationReference,
          projectReference = Some(projectReference),
          tags = tags,
          start = start,
          endOrDuration = endOrDuration
        )
    }
  }
}

object StartOrAddProjectBookingRequest {
  implicit val addBookingFormat: OFormat[StartOrAddProjectBookingRequest] =
    Json.format[StartOrAddProjectBookingRequest]
}
