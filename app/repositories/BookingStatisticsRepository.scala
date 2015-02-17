package repositories

import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent._
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.json.BSONFormats._
import play.api.libs.json._
import models._
import reactivemongo.core.commands.LastError
import play.api.Logger
import org.joda.time.DateTime
import repositories.MongoDBCommandSet._

trait BookingStatisticRepository[M <: models.BaseEntity[I], I <: com.tegonal.play.json.TypedId.BaseId[_]] extends BaseRepository[M, I] {
  def deleteStatistics(userId: UserId)(implicit format: play.api.libs.json.Format[M]): Future[Boolean]

  def findByUserIdAndRange(userId: UserId, from: DateTime, to: DateTime)(implicit format: play.api.libs.json.Format[M]): Future[Traversable[M]]
}

trait BookingByProjectRepository extends BookingStatisticRepository[BookingByProject, BookingByProjectId] {
}

trait BookingByCategoryRepository extends BookingStatisticRepository[BookingByCategory, BookingByCategoryId] {
}

trait BookingByTagRepository extends BookingStatisticRepository[BookingByTag, BookingByTagId] {
}

abstract class BookingStatisticMongoRepository[M <: models.BaseEntity[I], I <: com.tegonal.play.json.TypedId.BaseId[_]](implicit format: play.api.libs.json.Format[M]) extends BaseReactiveMongoRepository[M, I] with BookingStatisticRepository[M, I] {

  def deleteStatistics(userId: UserId)(implicit format: play.api.libs.json.Format[M]): Future[Boolean] = {
    val sel = Json.obj("userId" -> userId)
    find(sel) flatMap { res =>
      Future.sequence(res map {
        case (model, id) =>
          coll.remove(model)
      }) map { results =>
        results.filter(!_.ok).size == 0
      }
    }
  }

  def findByUserIdAndRange(userId: UserId, from: DateTime, to: DateTime)(implicit format: play.api.libs.json.Format[M]): Future[Traversable[M]] = {
    val sel = Json.obj("userId" -> userId,
      And -> Json.arr(Json.obj("start" -> Json.obj(GreaterOrEqualsThan -> from)),
        Json.obj("start" -> Json.obj(LowerOrEqualsThan -> to))))
    Logger.debug(s"findByUserAndRange:$sel")
    find(sel) map (_.map(_._1))
  }
}

class BookingByProjectMongoRepository extends BookingStatisticMongoRepository[BookingByProject, BookingByProjectId] with BookingByProjectRepository {
  def coll = db.collection[JSONCollection]("BookingByProject")
}

class BookingByCategoryMongoRepository extends BookingStatisticMongoRepository[BookingByCategory, BookingByCategoryId] with BookingByCategoryRepository {
  def coll = db.collection[JSONCollection]("BookingByCategory")
}

class BookingByTagMongoRepository extends BookingStatisticMongoRepository[BookingByTag, BookingByTagId] with BookingByTagRepository {
  def coll = db.collection[JSONCollection]("BookingByTag")
}