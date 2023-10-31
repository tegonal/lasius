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

case class AuthTokenId(value: UUID = UUID.randomUUID()) extends UUIDBaseId

object AuthTokenId {
  implicit val format: OFormat[AuthTokenId] =
    Json.format[AuthTokenId]
}

case class AuthToken(id: AuthTokenId, expiration: DateTime, user: UserReference)
    extends BaseEntity[AuthTokenId] {}

object AuthToken {
  implicit val format: OFormat[AuthToken] =
    Json.format[AuthToken]
}
