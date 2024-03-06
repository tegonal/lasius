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

import models.{UserId, _}
import mongo.EmbedMongo
import org.specs2.runner.JUnitRunner
import reactivemongo.api.bson.BSONObjectID

import scala.concurrent.Await
import scala.concurrent.duration._

class UserFavoritesRepositorySpec extends EmbedMongo {

  val repository = new UserFavoritesMongoRepository()
  "UserFavoritesRepository getById" should {
    "return empty favorites if no favorites where found" in {
      // initialize
      val user   = EntityReference(UserId(), "userUserFavoritesRepositorySpec")
      val teamId = OrganisationId()

      // execute
      val result = withDBSession()(implicit dbSession =>
        repository.getByUser(user, teamId)).awaitResult()

      // test
      result === UserFavorites(user.id, teamId, Seq())
    }
  }

  "UserFavoritesRepository addFavorite" should {
    "insert new userfavorites if no favorites did exist so far" in {
      // initialize
      val user   = EntityReference(UserId(), "userUserFavoritesRepositorySpec")
      val teamId = OrganisationId()

      val bookingStub =
        BookingStub(EntityReference(ProjectId(), "p1"),
                    Set(SimpleTag(TagId("tag1"))))

      // execute
      val result = withDBSession()(implicit dbSession =>
        repository
          .addFavorite(user,
                       teamId,
                       bookingStub.projectReference.get,
                       bookingStub.tags))
        .awaitResult()

      // test
      result === UserFavorites(user.id, teamId, Seq(bookingStub))
    }

    "add new userfavorites to existing favorites" in {
      // initialize
      val user   = EntityReference(UserId(), "userUserFavoritesRepositorySpec")
      val teamId = OrganisationId()
      val existingBookingStub =
        BookingStub(EntityReference(ProjectId(), "p2"),
                    Set(SimpleTag(TagId("tag2"))))
      val bookingStub =
        BookingStub(EntityReference(ProjectId(), "p1"),
                    Set(SimpleTag(TagId("tag1"))))

      withDBSession()(implicit dbSession =>
        repository.upsert(
          UserFavorites(user.id, teamId, Seq(existingBookingStub))))
        .awaitResult()

      // execute
      val result = withDBSession()(implicit dbSession =>
        repository
          .addFavorite(user,
                       teamId,
                       bookingStub.projectReference.get,
                       bookingStub.tags))
        .awaitResult()

      // test
      result === UserFavorites(user.id,
                               teamId,
                               Seq(existingBookingStub, bookingStub))
    }
  }

  "UserFavoritesRepository removeFavorite" should {
    "return empty sub if userid does not exists" in {
      // initialize
      val user   = EntityReference(UserId(), "userUserFavoritesRepositorySpec")
      val teamId = OrganisationId()
      val bookingStub =
        BookingStub(EntityReference(ProjectId(), "p1"),
                    Set(SimpleTag(TagId("tag1"))))

      // execute
      val result =
        withDBSession()(implicit dbSession =>
          repository.removeFavorite(user, teamId, bookingStub)).awaitResult()
      result === UserFavorites(user.id, teamId, Seq())
    }

    "remove booking stub" in {
      // initialize
      val user   = EntityReference(UserId(), "userUserFavoritesRepositorySpec")
      val teamId = OrganisationId()
      val bookingStub1 =
        BookingStub(EntityReference(ProjectId(), "p1"),
                    Set(SimpleTag(TagId("tag1"))))
      val bookingStub2 =
        BookingStub(EntityReference(ProjectId(), "p2"),
                    Set(SimpleTag(TagId("tag2"))))

      withDBSession()(implicit dbSession =>
        repository
          .upsert(
            UserFavorites(user.id, teamId, Seq(bookingStub1, bookingStub2))))
        .awaitResult()

      // execute
      val result =
        withDBSession()(implicit dbSession =>
          repository.removeFavorite(user, teamId, bookingStub1)).awaitResult()

      // test
      result === UserFavorites(user.id, teamId, Seq(bookingStub2))
    }

  }
}
