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
import domain.UserTimeBookingAggregate.UpdateBookingCommand
import models.OrganisationId.OrganisationReference
import models.ProjectId.ProjectReference
import org.joda.time.{DateTime, Duration}
import play.api.libs.json._
import models.BaseFormat._

import scala.concurrent.{ExecutionContext, Future}

/** updating of end date and duration are mutually exclusive
  */
case class UpdateProjectBookingRequest(projectId: Option[ProjectId] = None,
                                       tags: Option[Set[Tag]] = None,
                                       start: Option[DateTime] = None,
                                       end: Option[DateTime] = None,
                                       duration: Option[Duration] = None)
    extends Validation {
  def toCommand(bookingId: BookingId,
                organisationReference: OrganisationReference,
                projectReference: Option[ProjectReference])(implicit
      subject: Subject,
      executionContext: ExecutionContext): Future[UpdateBookingCommand] = {
    validateMutualExclusive("end_date" -> end, "duration" -> duration).map {
      endOrDuration =>
        UpdateBookingCommand(
          userReference = subject.userReference,
          organisationReference = organisationReference,
          bookingId = bookingId,
          projectReference = projectReference,
          tags = tags,
          start = start,
          endOrDuration = endOrDuration
        )
    }
  }
}

object UpdateProjectBookingRequest {
  implicit val editBookingFormat: OFormat[UpdateProjectBookingRequest] =
    Json.format[UpdateProjectBookingRequest]
}
