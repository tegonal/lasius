package controllers

import play.api.mvc.Controller
import repositories._
import models._
import play.api.libs.json._
import play.api.mvc.Action
import play.api.libs.concurrent.Execution.Implicits._

class UserFavoritesController {
  self: Controller with UserDataRepositoryComponent =>

  def getFavorites(userId: UserId) = Action.async {
    userFavoritesRepository.getByUser(userId) map { favorites =>
      Ok(Json.toJson(favorites))
    }
  }

  def addFavorite(userId: UserId, categoryId: CategoryId, projectId: ProjectId, tags: Seq[TagId]) = Action.async {
    userFavoritesRepository.addFavorite(userId, categoryId, projectId, tags) map { favorites =>
      Ok(Json.toJson(favorites))
    }
  }

  def removeFavorite(userId: UserId, categoryId: CategoryId, projectId: ProjectId, tags: Seq[TagId]) = Action.async {
    userFavoritesRepository.removeFavorite(userId, BookingStub(categoryId, projectId, tags)) map { favorites =>
      Ok(Json.toJson(favorites))
    }
  }
}

object UserFavoritesController extends UserFavoritesController with Controller with MongoUserDataRepositoryComponent