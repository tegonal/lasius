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
import org.joda.time.DateTime
import play.api.libs.json.{Format, Json, OFormat}
import scalaoauth2.provider.AccessToken

import java.util.UUID

case class OAuthAuthorizationCodeId(value: UUID = UUID.randomUUID())
    extends UUIDBaseId

object OAuthAuthorizationCodeId {
  implicit val format: Format[OAuthAuthorizationCodeId] =
    BaseFormat.idformat[OAuthAuthorizationCodeId](
      OAuthAuthorizationCodeId.apply _)
}

case class OAuthAuthorizationCode(id: OAuthAuthorizationCodeId,
                                  code: String,
                                  clientId: String,
                                  scope: Option[String],
                                  redirectUri: String,
                                  userId: OAuthUserId,
                                  createdAt: DateTime)
    extends BaseEntity[OAuthAuthorizationCodeId] {}

object OAuthAuthorizationCode {
  import models.BaseFormat._
  implicit val format: OFormat[OAuthAuthorizationCode] =
    Json.format[OAuthAuthorizationCode]
}
