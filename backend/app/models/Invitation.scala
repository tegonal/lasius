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

import julienrf.json.derived
import models.OrganisationId.OrganisationReference
import models.ProjectId.ProjectReference
import models.UserId.UserReference
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Format
import models.BaseFormat._

sealed trait Invitation extends BaseEntity[InvitationId] {
  val createDate: DateTime
  val createdBy: UserReference
  val expiration: DateTime
  val invitedEmail: String
  val outcome: Option[InvitationOutcome]
}

object Invitation {
  implicit val format: OFormat[Invitation] =
    derived.flat.oformat[Invitation](BaseFormat.defaultTypeFormat)
}

case class JoinOrganisationInvitation(
    id: InvitationId,
    invitedEmail: String,
    createDate: DateTime,
    createdBy: UserReference,
    expiration: DateTime,
    organisationReference: OrganisationReference,
    role: OrganisationRole,
    outcome: Option[InvitationOutcome],
    // type attribute only needed to generate correct swagger definition
    `type`: String = classOf[JoinOrganisationInvitation].getSimpleName)
    extends Invitation

object JoinOrganisationInvitation {
  val format: Format[JoinOrganisationInvitation] =
    Json.format[JoinOrganisationInvitation]
}

case class JoinProjectInvitation(
    id: InvitationId,
    invitedEmail: String,
    createDate: DateTime,
    createdBy: UserReference,
    expiration: DateTime,
    sharedByOrganisationReference: OrganisationReference,
    projectReference: ProjectReference,
    role: ProjectRole,
    outcome: Option[InvitationOutcome],
    // type attribute only needed to generate correct swagger definition
    `type`: String = classOf[JoinProjectInvitation].getSimpleName)
    extends Invitation

object JoinProjectInvitation {
  val format: Format[JoinProjectInvitation] = Json.format[JoinProjectInvitation]
}
