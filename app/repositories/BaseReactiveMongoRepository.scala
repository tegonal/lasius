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

import com.tegonal.play.json.TypedId.BaseId
import core.ReactiveMongoApiAware
import models.BaseEntity
import play.api.libs.json._
import play.api.libs.json.Json.JsValueWrapper
import reactivemongo.api._
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson._
import reactivemongo.play.json._
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.duration._

import scala.concurrent.{ExecutionContext, Future}

class MongoDBCommandException(msg: String) extends RuntimeException

trait BaseRepository[T <: BaseEntity[ID], ID <: BaseId[_]] {

  def failoverStrategy = FailoverStrategy(initialDelay = 100 milliseconds,
    retries = 16)

  def coll: Future[JSONCollection]

  def get(id: BSONObjectID): Future[Option[(T, BSONObjectID)]]

  def insert(t: T)(implicit ctx: ExecutionContext): Future[BSONObjectID]

  def insert(ts: List[T])(implicit ctx: ExecutionContext): Future[List[BSONObjectID]]

  def update(sel: JsObject, modifier: JsObject, upsert: Boolean = true)(implicit ctx: ExecutionContext): Future[Boolean]

  def update(doc: T)(implicit ctx: ExecutionContext): Future[Boolean]

  def find(sel: JsObject, limit: Int = -1, skip: Int = 0, sort: JsObject = Json.obj(), projection: JsObject = Json.obj())(implicit ctx: ExecutionContext): Future[Traversable[(T, BSONObjectID)]]

  def findFirst(sel: JsObject, skip: Int = 0)(implicit ctx: ExecutionContext): Future[Option[(T, BSONObjectID)]]

  def findByIds(ids: Traversable[BSONObjectID], limit: Int)(implicit ctx: ExecutionContext): Future[Traversable[(T, BSONObjectID)]]

  def findIds(sel: JsObject): Future[Seq[BSONObjectID]]

  def findById(id: ID)(implicit fact: ID => JsValueWrapper): Future[Option[T]]

  def remove(obj: T)(implicit ctx: ExecutionContext): Future[Boolean]

  def removeById(id: ID)(implicit fact: ID => JsValueWrapper, ctx: ExecutionContext): Future[Boolean]
}

abstract class BaseReactiveMongoRepository[T <: BaseEntity[ID], ID <: BaseId[_]](implicit ctx: ExecutionContext, format: Format[T]) {
  self: BaseRepository[T, ID] with ReactiveMongoApiAware =>

  def db = reactiveMongoApi.database

  lazy val bsonCollection: Future[BSONCollection] = db.flatMap(d => coll.map(c => d.collection(c.name, c.failoverStrategy)))

  def findIds(sel: JsObject): Future[Seq[BSONObjectID]] = {
    coll.flatMap(_.find(sel, Json.obj()).cursor[JsObject]().collect[Seq](Integer.MAX_VALUE, Cursor.FailOnError()).map(_.map(js => (js \ "_id").as[BSONObjectID])))
  }

  def findByIds(ids: Traversable[BSONObjectID], limit: Int = 0)(implicit ctx: ExecutionContext) = {
    val query = Json.obj("_id" -> Json.obj("$in" -> ids))
    find(query, limit)
  }

  def get(id: BSONObjectID): Future[Option[(T, BSONObjectID)]] = {
    coll.flatMap(_.find(Json.obj("_id" -> id)).cursor[JsObject]().headOption.map(_.map(js => (js.as[T], id))))
  }

  def update(sel: JsObject, modifier: JsObject, upsert: Boolean = true)(implicit ctx: ExecutionContext): Future[Boolean] = {
    coll.flatMap(_.update(sel, modifier, upsert = upsert).map(_.ok))
  }

  def update(doc: T)(implicit ctx: ExecutionContext): Future[Boolean] = {
    val json = format.writes(doc).as[JsObject]
    coll.flatMap(_.insert(ordered = false).one(json).map(_.ok))
  }

  def remove(obj: T)(implicit ctx: ExecutionContext): Future[Boolean] = {
    val json = format.writes(obj).as[JsObject]
    coll.flatMap(_.remove(json).map(_.ok))
  }

  def removeById(id: ID)(implicit fact: ID => JsValueWrapper, ctx: ExecutionContext): Future[Boolean] = {
    coll.flatMap(_.remove(Json.obj("id" -> id)).map(_.ok))
  }

  def insert(t: T)(implicit ctx: ExecutionContext): Future[BSONObjectID] = {
    val id = BSONObjectID.generate
    val obj = format.writes(t).as[JsObject]
    obj \ "_id" match {
      case _: JsUndefined =>
        coll.flatMap(_.insert(ordered = false).one(obj ++ Json.obj("_id" -> id)).map(_ => id))

      case JsDefined(JsObject(Seq((_, JsString(oid))))) =>
        coll.flatMap(_.insert(ordered = false).one(obj).map { _ => BSONObjectID(oid.getBytes) })

      case JsDefined(JsObject(Seq("$oid", JsString(oid)))) =>
        coll.flatMap(_.insert(ordered = false).one(obj).map { _ => BSONObjectID(oid.getBytes) })

      case JsDefined(JsString(oid)) =>
        coll.flatMap(_.insert(ordered = false).one(obj).map { _ => BSONObjectID(oid.getBytes) })

      case f => sys.error(s"Could not parse _id field: $f")
    }
  }

  def insert(ts: List[T])(implicit ctx: ExecutionContext): Future[List[BSONObjectID]] = {
    val objects = ts.map { t =>
      val obj = format.writes(t).as[JsObject]
      if((obj \"_id").isEmpty) {
        val id = BSONObjectID.generate
        (obj ++ Json.obj("_id" -> id), id)
      } else {
        (obj, extractId(obj))
      }
    }

    coll.flatMap(_.insert(true).many(objects.map(_._1)).map(_ => objects.map(_._2)))
  }

  private def extractId(obj: JsObject) = { obj \ "_id" match {
      case JsDefined(JsObject(Seq((_, JsString(oid))))) =>
        BSONObjectID(oid.getBytes)

      case JsDefined(JsObject(Seq("$oid", JsString(oid)))) =>
        BSONObjectID(oid.getBytes)

      case JsDefined(JsString(oid)) =>
        BSONObjectID(oid.getBytes)

      case f => sys.error(s"Could not parse _id field: $f")
    }
  }


  def find(sel: JsObject, limit: Int = -1, skip: Int = 0, sort: JsObject = Json.obj(), projection: JsObject = Json.obj())(implicit ctx: ExecutionContext): Future[Traversable[(T, BSONObjectID)]] = {
    coll.flatMap(_.find(sel).projection(projection).sort(sort).options(QueryOpts().skip(skip).batchSize(limit)).cursor[JsObject]().collect[Traversable](limit, Cursor.FailOnError()).map(_.map(js => (js.as[T], (js \ "_id").as[BSONObjectID]))))
  }

  def findFirst(sel: JsObject, skip: Int = 0)(implicit ctx: ExecutionContext): Future[Option[(T, BSONObjectID)]] = {
    find(sel, 1, skip) map (_.headOption)
  }

  def findById(id: ID)(implicit fact: ID => JsValueWrapper): Future[Option[T]] = {
    val sel = Json.obj("id" -> fact(id))
    find(sel) map (_.headOption.map(_._1))
  }
}