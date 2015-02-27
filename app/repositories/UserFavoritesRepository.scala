package repositories

import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent._
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.json.BSONFormats._
import play.api.libs.json._
import models._
import models.BaseFormat._
import repositories.MongoDBCommandSet._

trait UserFavoritesRepository extends BaseRepository[UserFavorites, UserId] {
  def getByUser(userId: UserId): Future[UserFavorites]

  def addFavorite(userId: UserId, categoryId: CategoryId, projectId: ProjectId, tags: Seq[TagId]): Future[UserFavorites]

  def removeFavorite(userId: UserId, bookingStub: BookingStub): Future[UserFavorites]
}

class UserFavoritesMongoRepository extends BaseReactiveMongoRepository[UserFavorites, UserId] with UserFavoritesRepository {
  def coll = db.collection[JSONCollection]("Favorites")

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
    update(Json.obj("id" -> userId), modifier, true) flatMap {
      case true => getByUser(userId)
      case _ => throw new RuntimeException("Couldn't remove favorites") //correct error handling?
    }
  }
}