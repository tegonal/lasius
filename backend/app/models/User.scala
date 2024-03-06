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
import play.api.libs.json._

import java.util.UUID

case class UserId(value: UUID = UUID.randomUUID()) extends UUIDBaseId

object UserId {
  implicit val idFormat: Format[UserId] =
    BaseFormat.idformat[UserId](UserId.apply _)

  type UserReference = EntityReference[UserId]
}

case class User(id: UserId,
                key: String,
                email: String,
                password: String,
                firstName: String,
                lastName: String,
                active: Boolean,
                role: UserRole,
                organisations: Seq[UserOrganisation],
                settings: Option[UserSettings])
    extends BaseEntity[UserId] {
  def reference: UserReference = EntityReference(id, key)

  def toDTO: UserDTO = UserDTO(
    id = id,
    key = key,
    email = email,
    firstName = firstName,
    lastName = lastName,
    active = active,
    role = role,
    organisations = organisations,
    settings = settings.getOrElse(
      UserSettings(
        lastSelectedOrganisation = None
      ))
  )

  def stub: UserStub = UserStub(id = id,
                                key = key,
                                email = email,
                                firstName = firstName,
                                lastName = lastName,
                                active = active,
                                role = role)
}

object User {
  implicit val userFormat: Format[User] = Json.format[User]
}

case class UserDTO(id: UserId,
                   key: String,
                   email: String,
                   firstName: String,
                   lastName: String,
                   active: Boolean,
                   role: UserRole,
                   organisations: Seq[UserOrganisation],
                   settings: UserSettings)

object UserDTO {
  implicit val userFormat: Format[UserDTO] = Json.format[UserDTO]
}

case class UserStub(id: UserId,
                    key: String,
                    email: String,
                    firstName: String,
                    lastName: String,
                    active: Boolean,
                    role: UserRole)

object UserStub {
  implicit val userFormat: Format[UserStub] = Json.format[UserStub]
}
