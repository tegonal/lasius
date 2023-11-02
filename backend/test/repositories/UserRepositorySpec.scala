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

package repositories

import models.{FreeUser, User, UserId}
import mongo.EmbedMongo

class UserRepositorySpec extends EmbedMongo {
  sequential =>
  val repository = new UserMongoRepository()
  "UserRepository findByEmail" should {
    "find user by email" in {
      val email = "email"
      val user = User(id = UserId(),
                      key = "user",
                      email = email,
                      firstName = "firstname",
                      lastName = "lastname",
                      active = true,
                      role = FreeUser,
                      organisations = Seq(),
                      settings = None)

      // initialize
      withDBSession()(implicit dbSession => repository.upsert(user))
        .awaitResult()

      val result = withDBSession()(implicit dbSession =>
        repository.findByEmail(email)).awaitResult()
      result === Some(user)
    }
    "find none" in {
      val email = "email"
      val user = User(id = UserId(),
                      key = "user",
                      email = email,
                      firstName = "firstname",
                      lastName = "lastname",
                      active = true,
                      role = FreeUser,
                      organisations = Seq(),
                      settings = None)

      // initialize
      withDBSession()(implicit dbSession => repository.upsert(user))
        .awaitResult()

      val result = withDBSession()(implicit dbSession =>
        repository.findByEmail("email2")).awaitResult()
      result === None
    }
  }
}
