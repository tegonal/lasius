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

import models.BaseFormat.UUIDBaseId
import models.UserId.UserReference
import org.joda.time.DateTime
import play.api.libs.json.{Json, OFormat}

import java.util.UUID

case class AccessTokenId(value: UUID = UUID.randomUUID()) extends UUIDBaseId

object AccessTokenId {
  implicit val format: OFormat[AccessTokenId] =
    Json.format[AccessTokenId]
}

case class AccessToken(id: AccessTokenId,
                       name: String,
                       tokenSecretHash: String,
                       expiration: DateTime,
                       user: UserReference)
    extends BaseEntity[AccessTokenId] {}

object AccessToken {
  implicit val format: OFormat[AccessToken] =
    Json.format[AccessToken]
}
