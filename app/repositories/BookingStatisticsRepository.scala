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

  def add(model: M)(implicit writes: Writes[I]): Future[M]

  def subtract(model: M)(implicit writes: Writes[I]): Future[Option[M]]
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

  def add(model: M)(implicit writes: Writes[I]): Future[M] = {
    val sel = getUniqueContraint(model)
    Logger.debug(s"add [$sel]:$model")
    findFirst(sel) flatMap {
      _.map { o =>
        o match {
          case (current, id) =>
            val newModel = current + model
            update(newModel) map {
              case true =>
                newModel
              case _ => current
            }
          case _ =>
            Logger.debug(s"add [$sel]: no model found to add to: ${model}:$sel, create new one2")
            insert(model) map (id => model)
        }
      }.getOrElse {
        Logger.debug(s"add [$sel]: no model found to add to: ${model}:$sel, create new one")
        insert(model).map(id => model)
      }
    }
  }

  def subtract(model: M)(implicit writes: Writes[I]): Future[Option[M]] = {
    val sel = getUniqueContraint(model)
    Logger.debug(s"subtract [$sel]:$model")
    findFirst(sel) flatMap {
      _.map { o =>
        o match {
          case (current, _) =>
            Logger.debug(s"subtract [$sel]:$current - $model")
            val duration = if (current.duration.getMillis < model.duration.getMillis) { 0 } else { current.duration.getMillis - model.duration.getMillis }
            val newModel = current.duration(Duration.millis(duration))
            Logger.debug(s"subtract [$sel]:result=$newModel")
            update(newModel) map {
              case true =>
                Some(newModel)
              case _ => None
            }
          case e =>
            Logger.warn(s"subtract [$sel]: no model found to subtract from: ${model}:$sel - $e")
            Future.successful(None)
        }
      }.getOrElse {
        Logger.warn(s"subtract [$sel]: no model found to subtract from: ${model}:$sel")
        Future.successful(None)
      }
    }
  }

  def getUniqueContraint(model: M): JsObject
}

class BookingByProjectMongoRepository extends BookingStatisticMongoRepository[BookingByProject, BookingByProjectId] with BookingByProjectRepository {
  def coll = db.collection[JSONCollection]("BookingByProject")

  def getUniqueContraint(model: BookingByProject): JsObject = {
    Json.obj("day" -> model.day, "projectId" -> model.projectId)
  }
}

class BookingByCategoryMongoRepository extends BookingStatisticMongoRepository[BookingByCategory, BookingByCategoryId] with BookingByCategoryRepository {
  def coll = db.collection[JSONCollection]("BookingByCategory")

  def getUniqueContraint(model: BookingByCategory): JsObject = {
    Json.obj("day" -> model.day, "categoryId" -> model.categoryId)
  }
}

class BookingByTagMongoRepository extends BookingStatisticMongoRepository[BookingByTag, BookingByTagId] with BookingByTagRepository {
  def coll = db.collection[JSONCollection]("BookingByTag")

  def getUniqueContraint(model: BookingByTag): JsObject = {
    Json.obj("day" -> model.day, "tagId" -> model.tagId)
  }
}