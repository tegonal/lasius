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
package controllers

import play.api.mvc.Controller
import repositories._
import models._
import play.api.libs.json._
import play.api.mvc.Action
import play.api.libs.concurrent.Execution.Implicits._
import actors.ClientMessagingWebsocketActor

class UserFavoritesController {
  self: Controller with UserDataRepositoryComponent with Security =>

  def getFavorites() = HasRole(FreeUser, parse.empty) {
    implicit subject =>
      implicit request => {
        userFavoritesRepository.getByUser(subject.userId) map { favorites =>
          Ok(Json.toJson(favorites))
        }
      }
  }

  def addFavorite(categoryId: CategoryId, projectId: ProjectId, tags: Seq[TagId]) = HasRole(FreeUser, parse.empty) {
    implicit subject =>
      implicit request => {
        userFavoritesRepository.addFavorite(subject.userId, categoryId, projectId, tags) map { favorites =>
          ClientMessagingWebsocketActor ! (subject.userId, FavoriteAdded(subject.userId, BookingStub(categoryId, projectId, tags)), List(subject.userId))
          Ok(Json.toJson(favorites))
        }
      }
  }

  def removeFavorite(categoryId: CategoryId, projectId: ProjectId, tags: Seq[TagId]) = HasRole(FreeUser, parse.empty) {
    implicit subject =>
      implicit request => {
        userFavoritesRepository.removeFavorite(subject.userId, BookingStub(categoryId, projectId, tags)) map { favorites =>
          ClientMessagingWebsocketActor ! (subject.userId, FavoriteRemoved(subject.userId, BookingStub(categoryId, projectId, tags)), List(subject.userId))
          Ok(Json.toJson(favorites))
        }
      }
  }
}

object UserFavoritesController extends UserFavoritesController with Controller with MongoUserDataRepositoryComponent with Security with DefaultSecurityComponent