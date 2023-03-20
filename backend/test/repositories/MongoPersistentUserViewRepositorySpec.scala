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

import models.LocalDateTimeWithTimeZone.DateTimeHelper
import models._
import mongo.EmbedMongo
import org.joda.time.DateTime
import util.Awaitable
import play.api.libs.json._
import reactivemongo.api.bson.BSONObjectID

class MongoPeristentUserViewRepositorySpec extends EmbedMongo with Awaitable {

  val repository = new BookingHistoryMongoRepository()
  "BookingV2 history delete" should {
    "delete all history entries per user" in {
      val user1 =
        EntityReference(UserId(), "userMongoPeristentUserViewRepositorySpec1")
      val user2 =
        EntityReference(UserId(), "userMongoPeristentUserViewRepositorySpec2")

      val team = EntityReference(OrganisationId(), "team1")

      // initialize
      withDBSession() { implicit dbSession =>
        for {
          id1 <- repository.upsert(
            BookingV2(BookingId(),
                      DateTime.now().toLocalDateTimeWithZone(),
                      None,
                      user1,
                      team,
                      EntityReference(ProjectId(), "p1"),
                      Set()))
          id2 <- repository.upsert(
            BookingV2(BookingId(),
                      DateTime.now().toLocalDateTimeWithZone(),
                      None,
                      user1,
                      team,
                      EntityReference(ProjectId(), "p2"),
                      Set()))
          id3 <- repository.upsert(
            BookingV2(BookingId(),
                      DateTime.now().toLocalDateTimeWithZone(),
                      None,
                      user2,
                      team,
                      EntityReference(ProjectId(), "p3"),
                      Set()))
        } yield {
          Seq(id1, id2, id3)
        }
      }.awaitResult()

      val findAll = withDBSession()(implicit dbSession =>
        repository.find(Json.obj())).awaitResult()
      findAll must have size (3)

      val afterDelete = withDBSession()(implicit dbSession =>
        repository.deleteByUserReference(user1)).awaitResult()
      afterDelete must equalTo(true)

      val findAll2 = withDBSession()(implicit dbSession =>
        repository.find(Json.obj())).awaitResult()
      findAll2 must have size (1)
    }
  }
}
