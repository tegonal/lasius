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
import play.api.libs.json._
import scalaoauth2.provider.AuthInfo

import java.util.UUID

case class OAuthUserId(value: UUID = UUID.randomUUID()) extends UUIDBaseId

object OAuthUserId {
  implicit val idFormat: Format[OAuthUserId] =
    BaseFormat.idformat[OAuthUserId](OAuthUserId.apply _)
}

case class OAuthUser(id: OAuthUserId,
                     email: String,
                     password: String,
                     firstName: Option[String],
                     lastName: Option[String],
                     active: Boolean)
    extends BaseEntity[OAuthUserId] {
  def toAuthInfo(accessToken: OAuthAccessToken): AuthInfo[OAuthUser] =
    AuthInfo(
      user = this,
      clientId = accessToken.clientId,
      scope = accessToken.scope,
      redirectUri = None
    )

  def toAuthInfo(code: OAuthAuthorizationCode): AuthInfo[OAuthUser] =
    AuthInfo(
      user = this,
      clientId = Some(code.clientId),
      scope = code.scope,
      redirectUri = Some(code.redirectUri)
    )
}

object OAuthUser {
  implicit val userFormat: Format[OAuthUser] = Json.format[OAuthUser]
}
