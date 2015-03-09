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