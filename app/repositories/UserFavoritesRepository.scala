/*   __                          __                                          *\
*   / /____ ___ ____  ___  ___ _/ /       lasius                      *
*  / __/ -_) _ `/ _ \/ _ \/ _ `/ /        contributed by tegonal              *
*  \__/\__/\_, /\___/_//_/\_,_/_/         http://tegonal.com/                 *
*         /___/                                                               *
*                                                                             *
* This program is free software: you can redistribute it and/or modify it     *
* under the terms of the GNU General Public License as published by    *
* the Free Software Foundation, either version 3 of the License,              *
* or (at your option) any later version.                                      *
*                                                                             *
* This program is distributed in the hope that it will be useful, but         *
* WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY  *
* or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for *
* more details.                                                               *
*                                                                             *
* You should have received a copy of the GNU General Public License along     *
* with this program. If not, see http://www.gnu.org/licenses/                 *
*                                                                             *
\*                                                                           */
package repositories

import models._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json._
import reactivemongo.play.json.collection.JSONCollection
import repositories.MongoDBCommandSet._

import scala.concurrent._

trait UserFavoritesRepository extends BaseRepository[UserFavorites, UserId] {
  def getByUser(userId: UserId): Future[UserFavorites]

  def addFavorite(userId: UserId, categoryId: CategoryId, projectId: ProjectId, tags: Seq[TagId]): Future[UserFavorites]

  def removeFavorite(userId: UserId, bookingStub: BookingStub): Future[UserFavorites]
}

class UserFavoritesMongoRepository extends BaseReactiveMongoRepository[UserFavorites, UserId] with UserFavoritesRepository {
  def coll = db.map(_.collection[JSONCollection]("Favorites"))

  def getByUser(userId: UserId): Future[UserFavorites] = {
    findById(userId) map { favorites =>
      favorites.getOrElse(UserFavorites(userId, Seq()))
    }
  }

  def addFavorite(userId: UserId, categoryId: CategoryId, projectId: ProjectId, tags: Seq[TagId]): Future[UserFavorites] = {
    val stub = BookingStub(categoryId, projectId, tags)
    val modifier = Json.obj("favorites" -> stub)
    findById(userId) flatMap {
      case Some(favorites) => update(Json.obj("id" -> userId), Json.obj(AddToSet -> modifier), true) map {
        case true => favorites.copy(favorites = favorites.favorites :+ stub)
        case _ => throw new RuntimeException("Couldn't update favorites") //correct error handling?
      }
      case None =>
        val newFavorites = UserFavorites(userId, Seq(stub))
        insert(newFavorites) map { id => newFavorites }
    }
  }

  def removeFavorite(userId: UserId, bookingStub: BookingStub): Future[UserFavorites] = {
    val modifier = Json.obj(Pull -> Json.obj("favorites" -> bookingStub))
    update(Json.obj("id" -> userId), modifier, false) flatMap {
      case true => getByUser(userId)
      case _ => throw new RuntimeException("Couldn't remove favorites") //correct error handling?
    }
  }
}