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
import core.Validation.ValidationFailedException
import domain.UserTimeBookingAggregate.{
  AddBookingCommand,
  UserTimeBookingCommand
}
import models.OrganisationId.OrganisationReference
import org.joda.time.{DateTime, Duration}
import play.api.libs.json.{Json, OFormat}
import models.BaseFormat._

import scala.concurrent.{ExecutionContext, Future}

case class AddAbsenceBookingRequest(bookingType: NonWorkingTimeBooking,
                                    start: DateTime,
                                    tags: Set[Tag],
                                    end: Option[DateTime] = None,
                                    duration: Option[Duration] = None)
    extends Validation {
  def toCommand(organisationReference: OrganisationReference)(implicit
      subject: Subject,
      executionContext: ExecutionContext): Future[UserTimeBookingCommand] = {
    validateMutualExclusive("end_date" -> end, "duration" -> duration).flatMap {
      case Some(endOrDuration) =>
        Future.successful(
          AddBookingCommand(
            bookingType = bookingType,
            userReference = subject.userReference,
            organisationReference = organisationReference,
            projectReference = None,
            tags = tags,
            start = start,
            endOrDuration = endOrDuration
          ))
      case _ =>
        Future.failed(
          ValidationFailedException(
            "Need to specify either 'end' or 'duration'"))
    }
  }
}

object AddAbsenceBookingRequest {
  implicit val addBookingFormat: OFormat[AddAbsenceBookingRequest] =
    Json.format[AddAbsenceBookingRequest]
}
