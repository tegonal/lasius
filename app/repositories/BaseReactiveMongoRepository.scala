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

import scala.concurrent.ExecutionContext
import play.api.libs.json._
import scala.concurrent.Future
import play.api.libs.iteratee.Enumerator
import play.api.Logger
import play.api.Play.current
import com.tegonal.play.json.TypedId.BaseId
import models.BaseEntity
import play.api.libs.json.Json.JsValueWrapper
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api._
import reactivemongo.api.commands._
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson._

class MongoDBCommandException(msg: String) extends RuntimeException

trait BaseRepository[T <: BaseEntity[ID], ID <: BaseId[_]] {

  def coll: JSONCollection

  def get(id: BSONObjectID): Future[Option[(T, BSONObjectID)]]

  def insert(t: T)(implicit ctx: ExecutionContext): Future[BSONObjectID]

  def update(sel: JsObject, modifier: JsObject, upsert: Boolean = true)(implicit ctx: ExecutionContext): Future[Boolean]

  def update(doc: T)(implicit ctx: ExecutionContext): Future[Boolean]

  def find(sel: JsObject, limit: Int = 0, skip: Int = 0, sort: JsObject = Json.obj(), projection: JsObject = Json.obj())(implicit ctx: ExecutionContext): Future[Traversable[(T, BSONObjectID)]]

  def findFirst(sel: JsObject, skip: Int = 0)(implicit ctx: ExecutionContext): Future[Option[(T, BSONObjectID)]]

  def findStream(sel: JsObject, skip: Int = 0, pageSize: Int = 0)(implicit ctx: ExecutionContext): Enumerator[TraversableOnce[(T, BSONObjectID)]]

  def findByIds(ids: Traversable[BSONObjectID], limit: Int)(implicit ctx: ExecutionContext): Future[Traversable[(T, BSONObjectID)]]

  def findIds(sel: JsObject): Future[Seq[BSONObjectID]]

  def findById(id: ID)(implicit fact: ID => JsValueWrapper): Future[Option[T]]

  def remove(obj: T)(implicit ctx: ExecutionContext): Future[Boolean]

  //def update(obj: T)(implicit fact: ID => JsValueWrapper): Future[LastError]
}

abstract class BaseReactiveMongoRepository[T <: BaseEntity[ID], ID <: BaseId[_]](implicit ctx: ExecutionContext, format: Format[T]) {
  self: BaseRepository[T, ID] =>
  import play.modules.reactivemongo.json._
  import play.modules.reactivemongo.json.collection._

  lazy val reactiveMongoApi = current.injector.instanceOf[ReactiveMongoApi]
  def db = reactiveMongoApi.db

  lazy val bsonCollection: BSONCollection = db.collection(coll.name, coll.failoverStrategy)

  def findIds(sel: JsObject): Future[Seq[BSONObjectID]] = {
    val cursor = coll.find(sel, Json.obj()).cursor[JsObject]
    val list = cursor.collect[Seq]()
    list map (_ map (js => (js \ "_id").as[BSONObjectID]))
  }

  def findByIds(ids: Traversable[BSONObjectID], limit: Int = 0)(implicit ctx: ExecutionContext) = {
    val query = Json.obj("_id" -> Json.obj("$in" -> ids))
    find(query, limit)
  }

  def get(id: BSONObjectID): Future[Option[(T, BSONObjectID)]] = {
    coll.find(Json.obj("_id" -> id)).cursor[JsObject].headOption.map(_.map(js => (js.as[T], id)))
  }

  def update(sel: JsObject, modifier: JsObject, upsert: Boolean = true)(implicit ctx: ExecutionContext): Future[Boolean] = {
    coll.update(sel, modifier, upsert = upsert) map (_.ok)
  }

  def update(doc: T)(implicit ctx: ExecutionContext): Future[Boolean] = {
    coll.save(doc) map (_.ok)
  }

  def remove(obj: T)(implicit ctx: ExecutionContext): Future[Boolean] = {
    val json = format.writes(obj).as[JsObject]
    coll.remove(json) map (_.ok)
  }

  def insert(t: T)(implicit ctx: ExecutionContext): Future[BSONObjectID] = {
    val id = BSONObjectID.generate
    val obj = format.writes(t).as[JsObject]
    obj \ "_id" match {
      case _: JsUndefined =>
        coll.insert(obj ++ Json.obj("_id" -> id))
          .map { _ => id }

      case JsDefined(JsObject(Seq((_, JsString(oid))))) =>
        coll.insert(obj).map { _ => BSONObjectID(oid) }

      case JsDefined(JsObject(Seq("$oid", JsString(oid)))) =>
        coll.insert(obj).map { _ => BSONObjectID(oid) }
              
      case JsDefined(JsString(oid)) =>
        coll.insert(obj).map { _ => BSONObjectID(oid) }

      case f => sys.error(s"Could not parse _id field: $f")
    }
  }

  def find(sel: JsObject, limit: Int = 0, skip: Int = 0, sort: JsObject = Json.obj(), projection: JsObject = Json.obj())(implicit ctx: ExecutionContext): Future[Traversable[(T, BSONObjectID)]] = {

    val cursor = coll.find(sel).projection(projection).sort(sort).options(QueryOpts().skip(skip).batchSize(limit)).cursor[JsObject]()
    val l = if (limit != 0) cursor.collect[Traversable](limit) else cursor.collect[Traversable]()
    l.map(_.map(js => (js.as[T], (js \ "_id").as[BSONObjectID])))
  }

  def findFirst(sel: JsObject, skip: Int = 0)(implicit ctx: ExecutionContext): Future[Option[(T, BSONObjectID)]] = {
    find(sel, 1, skip) map (_.headOption)
  }

  def findStream(sel: JsObject, skip: Int = 0, pageSize: Int = 0)(implicit ctx: ExecutionContext): Enumerator[TraversableOnce[(T, BSONObjectID)]] = {
    val cursor = coll.find(sel).options(QueryOpts().skip(skip)).cursor[JsObject]()
    val enum = if (pageSize != 0) cursor.enumerateBulks(pageSize) else cursor.enumerateBulks()
    enum.map(_.map(js => (js.as[T], (js \ "_id").as[BSONObjectID])))
  }

  def findById(id: ID)(implicit fact: ID => JsValueWrapper): Future[Option[T]] = {
    val sel = Json.obj("id" -> fact(id))
    find(sel) map (_.headOption.map(_._1))
  }
}