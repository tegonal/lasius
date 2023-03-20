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

import com.google.inject.ImplementedBy
import core.DBSession
import models.ProjectId.ProjectReference
import models.UserId.UserReference

import javax.inject.Inject
import models._
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.bson.collection.BSONCollection
import repositories.MongoDBCommandSet._

import scala.concurrent._

@ImplementedBy(classOf[UserFavoritesMongoRepository])
trait UserFavoritesRepository extends BaseRepository[UserFavorites, UserId] {
  def getByUser(userReference: UserReference, orgId: OrganisationId)(implicit
      dbSession: DBSession): Future[UserFavorites]

  def addFavorite(userReference: UserReference,
                  orgId: OrganisationId,
                  projectReference: ProjectReference,
                  tags: Set[Tag])(implicit
      dbSession: DBSession): Future[UserFavorites]

  def removeFavorite(userReference: UserReference,
                     orgId: OrganisationId,
                     bookingStub: BookingStub)(implicit
      dbSession: DBSession): Future[UserFavorites]
}

class UserFavoritesMongoRepository @Inject() (
    override implicit protected val executionContext: ExecutionContext)
    extends BaseReactiveMongoRepository[UserFavorites, UserId]
    with UserFavoritesRepository {
  override protected[repositories] def coll(implicit
      dbSession: DBSession): BSONCollection =
    dbSession.db.collection[BSONCollection]("Favorites")

  override def getByUser(userReference: UserReference, orgId: OrganisationId)(
      implicit dbSession: DBSession): Future[UserFavorites] = {
    val sel = Json.obj("id" -> userReference.id, "orgId" -> orgId)
    find(sel).map(_.headOption.map(_._1)).map { favorites =>
      favorites.getOrElse(UserFavorites(userReference.id, orgId, Seq()))
    }
  }

  def addFavorite(userReference: UserReference,
                  orgId: OrganisationId,
                  projectReference: ProjectReference,
                  tags: Set[Tag])(implicit
      dbSession: DBSession): Future[UserFavorites] = {
    val stub     = BookingStub(projectReference, tags)
    val modifier = Json.obj("favorites" -> stub)
    findById(userReference.id).flatMap {
      case Some(favorites) =>
        update(Json.obj("id"     -> userReference.id, "orgId" -> orgId),
               Json.obj(AddToSet -> modifier))
          .map {
            case true =>
              if (favorites.favorites.contains(stub)) favorites
              else favorites.copy(favorites = favorites.favorites :+ stub)
            case _ =>
              throw new RuntimeException(
                "Couldn't update favorites"
              ) // correct error handling?
          }
      case None =>
        val newFavorites = UserFavorites(userReference.id, orgId, Seq(stub))
        upsert(newFavorites).map { _ =>
          newFavorites
        }
    }
  }

  def removeFavorite(userReference: UserReference,
                     orgId: OrganisationId,
                     bookingStub: BookingStub)(implicit
      dbSession: DBSession): Future[UserFavorites] = {
    val modifier = Json.obj(Pull -> Json.obj("favorites" -> bookingStub))
    update(Json.obj("id" -> userReference.id, "orgId" -> orgId),
           modifier,
           upsert = false)
      .flatMap {
        case true => getByUser(userReference, orgId)
        case _ =>
          throw new RuntimeException(
            "Couldn't remove favorites"
          ) // correct error handling?
      }
  }
}
