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

import ai.x.play.json.Jsonx
import play.api.libs.json.{Format, Json}

sealed trait InvitationStatus

object InvitationStatus {

  import ai.x.play.json.SingletonEncoder.simpleName
  import ai.x.play.json.implicits.formatSingleton

  implicit val format: Format[InvitationStatus] =
    Jsonx.formatSealed[InvitationStatus]
}

case object UnregisteredUser extends InvitationStatus

case object InvitationOk extends InvitationStatus

case class InvitationStatusResponse(status: InvitationStatus,
                                    invitation: Invitation)

object InvitationStatusResponse {
  implicit val format: Format[InvitationStatusResponse] =
    Json.format[InvitationStatusResponse]
}