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
import models.BaseFormat._
import org.joda.time.Duration

trait BookingStatisticRepository[M <: models.OperatorEntity[I, M], I <: com.tegonal.play.json.TypedId.BaseId[_]] extends BaseRepository[M, I]
  with PersistentUserViewRepository[M, I] {

  def findByUserIdAndRange(userId: UserId, from: DateTime, to: DateTime)(implicit format: play.api.libs.json.Format[M]): Future[Traversable[M]]

  def add(model: M)(implicit writes: Writes[I]): Future[Boolean]

  def subtract(model: M)(implicit writes: Writes[I]): Future[Boolean]
}

trait BookingByProjectRepository extends BookingStatisticRepository[BookingByProject, BookingByProjectId] {
}

trait BookingByCategoryRepository extends BookingStatisticRepository[BookingByCategory, BookingByCategoryId] {
}

trait BookingByTagRepository extends BookingStatisticRepository[BookingByTag, BookingByTagId] {
}

abstract class BookingStatisticMongoRepository[M <: models.OperatorEntity[I, M], I <: com.tegonal.play.json.TypedId.BaseId[_]](implicit format: play.api.libs.json.Format[M]) extends BaseReactiveMongoRepository[M, I] with BookingStatisticRepository[M, I]
  with MongoPeristentUserViewRepository[M, I] {

  def findByUserIdAndRange(userId: UserId, from: DateTime, to: DateTime)(implicit format: play.api.libs.json.Format[M]): Future[Traversable[M]] = {
    val sel = Json.obj("userId" -> userId,
      And -> Json.arr(Json.obj("day" -> Json.obj(GreaterOrEqualsThan -> from)),
        Json.obj("day" -> Json.obj(LowerOrEqualsThan -> to))))
    Logger.debug(s"findByUserAndRange:$this:$sel")
    find(sel) map (_.map(_._1))
  }

  def add(model: M)(implicit writes: Writes[I]): Future[Boolean] = {
    val sel = getUniqueContraint(model)
    Logger.debug(s"add [$sel]:$model")
    update(sel, Json.obj(Inc -> Json.obj("duration" -> model.duration)), true)
  }

  def subtract(model: M)(implicit writes: Writes[I]): Future[Boolean] = {
    val sel = getUniqueContraint(model)
    Logger.debug(s"subtract [$sel]:$model")
    update(sel, Json.obj(Inc -> Json.obj("duration" -> Duration.ZERO.minus(model.duration))), true)
  }

  def getUniqueContraint(model: M): JsObject
}

class BookingByProjectMongoRepository extends BookingStatisticMongoRepository[BookingByProject, BookingByProjectId] with BookingByProjectRepository {
  def coll = db.collection[JSONCollection]("BookingByProject")

  def getUniqueContraint(model: BookingByProject): JsObject = {
    Json.obj("userId" -> model.userId, "day" -> model.day, "projectId" -> model.projectId)
  }
}

class BookingByCategoryMongoRepository extends BookingStatisticMongoRepository[BookingByCategory, BookingByCategoryId] with BookingByCategoryRepository {
  def coll = db.collection[JSONCollection]("BookingByCategory")

  def getUniqueContraint(model: BookingByCategory): JsObject = {
    Json.obj("userId" -> model.userId, "day" -> model.day, "categoryId" -> model.categoryId)
  }
}

class BookingByTagMongoRepository extends BookingStatisticMongoRepository[BookingByTag, BookingByTagId] with BookingByTagRepository {
  def coll = db.collection[JSONCollection]("BookingByTag")

  def getUniqueContraint(model: BookingByTag): JsObject = {
    Json.obj("userId" -> model.userId, "day" -> model.day, "tagId" -> model.tagId)
  }
}