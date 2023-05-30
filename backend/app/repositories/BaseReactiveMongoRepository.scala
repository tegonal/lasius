/*
 *
 * Lasius - Open source time tracker for teams
 * Copyright (c) Tegonal Genossenschaft (https://tegonal.com)
 *
 * This file is part of Lasius.
 *
 * Lasius is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * Lasius is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Lasius. If not, see <https://www.gnu.org/licenses/>.
 */

package repositories

import com.tegonal.play.json.TypedId.BaseId
import core.{DBSession, ExecutionContextAware}
import helpers.FutureHelper
import models.{BaseEntity, BaseFormat}
import play.api.libs.json.Json.JsValueWrapper
import play.api.libs.json._
import reactivemongo.api._
import reactivemongo.api.bson._
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.play.json.compat._
import reactivemongo.play.json.compat.json2bson._

import scala.concurrent.Future
// Conversions from BSON to JSON extended syntax
import reactivemongo.play.json.compat.bson2json._
import reactivemongo.play.json.compat.lax._

import scala.language.postfixOps

// Conversions from JSON to BSON
import reactivemongo.play.json.compat.json2bson._
import scala.concurrent.duration._

class MongoDBCommandException(msg: String) extends RuntimeException

trait BaseRepository[T <: BaseEntity[ID], ID <: BaseId[_]]
    extends ExecutionContextAware {

  protected[repositories] def coll(implicit
      dbSession: DBSession): BSONCollection

  def failoverStrategy: FailoverStrategy =
    FailoverStrategy(initialDelay = 100 milliseconds, retries = 16)

  def upsert(
      t: T)(implicit writer: Writes[ID], dbSession: DBSession): Future[Unit]

  def bulkInsert(ts: List[T])(implicit
      dbSession: DBSession): Future[List[BSONObjectID]]

  def findAll(limit: Int = -1)(implicit
      dbSession: DBSession): Future[Iterable[(T, BSONObjectID)]]

  def findFirst(sel: JsObject, skip: Int = 0)(implicit
      dbSession: DBSession): Future[Option[(T, BSONObjectID)]]

  def findById(id: ID)(implicit
      fact: ID => JsValueWrapper,
      dbSession: DBSession): Future[Option[T]]

  def remove(obj: T)(implicit dbSession: DBSession): Future[Boolean]

  def removeById(id: ID)(implicit
      fact: ID => JsValueWrapper,
      dbSession: DBSession): Future[Boolean]
}

trait DropAllSupport[T <: BaseEntity[ID], ID <: BaseId[_]] {
  self: BaseRepository[T, ID] =>
  def dropAll()(implicit dbSession: DBSession): Future[Boolean]
}

trait MongoDropAllSupport[T <: BaseEntity[ID], ID <: BaseId[_]]
    extends DropAllSupport[T, ID] {
  self: BaseReactiveMongoRepository[T, ID] with BaseRepository[T, ID] =>
  override def dropAll()(implicit dbSession: DBSession): Future[Boolean] =
    coll.drop(false)
}

abstract class BaseReactiveMongoRepository[T <: BaseEntity[ID],
                                           ID <: BaseId[_]](implicit
    format: Format[T])
    extends FutureHelper {
  self: BaseRepository[T, ID] =>

  protected[repositories] def findIds(sel: JsObject)(implicit
      dbSession: DBSession): Future[Seq[BSONObjectID]] =
    coll
      .find(sel, Some(Json.obj()))
      .cursor[JsObject]()
      .collect[Seq](Integer.MAX_VALUE, Cursor.FailOnError())
      .map(_.map(js => (js \ "_id").as[BSONObjectID]))

  protected[repositories] def findByIds(ids: Iterable[BSONObjectID],
                                        limit: Int = 0)(implicit
      dbSession: DBSession): Future[Iterable[(T, BSONObjectID)]] = {
    val query = Json.obj("_id" -> Json.obj("$in" -> ids))
    find(query, limit)
  }

  protected[repositories] def get(id: BSONObjectID)(implicit
      dbSession: DBSession): Future[Option[(T, BSONObjectID)]] = {
    coll
      .find(Json.obj("_id" -> id), Option.empty[JsObject])
      .cursor[JsObject]()
      .headOption
      .map(_.map(js => (js.as[T], id)))
  }

  protected[repositories] def updateFields(
      sel: JsObject,
      fields: Seq[(String, JsValueWrapper)])(implicit
      dbSession: DBSession): Future[Boolean] = {
    update(sel,
           Json.obj(MongoDBCommandSet.Set -> Json.obj(fields: _*)),
           upsert = false)
  }

  protected[repositories] def update(sel: JsObject,
                                     modifier: JsObject,
                                     upsert: Boolean = true,
                                     multi: Boolean = false,
                                     collation: Option[Collation] = None,
                                     arrayFilters: Seq[JsObject] = Seq())(
      implicit dbSession: DBSession): Future[Boolean] = {

    val af = arrayFilters.map(_.as[BSONDocument])

    coll
      .update(ordered = true)
      .one(sel,
           modifier,
           upsert,
           multi = multi,
           collation = collation,
           arrayFilters = af)
      .map(_.writeErrors.isEmpty)
  }

  protected[repositories] def remove(sel: JsObject)(implicit
      dbSession: DBSession): Future[Boolean] = {
    coll
      .delete(ordered = true)
      .one(sel, limit = None, collation = None)
      .map(_.writeErrors.isEmpty)
  }

  def remove(t: T)(implicit dbSession: DBSession): Future[Boolean] = {
    remove(format.writes(t).as[JsObject])
  }

  def removeById(id: ID)(implicit
      fact: ID => JsValueWrapper,
      dbSession: DBSession): Future[Boolean] = {
    remove(Json.obj("id" -> id))
  }

  def upsert(
      t: T)(implicit writer: Writes[ID], dbSession: DBSession): Future[Unit] = {
    val obj = format.writes(t).as[JsObject]
    coll
      .update(ordered = true)
      .one(Json.obj("id" -> t.id), obj, upsert = true)
      .map(_ => ())
  }

  def bulkInsert(ts: List[T])(implicit
      dbSession: DBSession): Future[List[BSONObjectID]] = {
    val objects = ts.map { t =>
      val obj = format.writes(t).as[JsObject]
      if ((obj \ "_id").isEmpty) {
        val id = BSONObjectID.generate()
        (obj ++ Json.obj("_id" -> id), id)
      } else {
        (obj, extractId(obj))
      }
    }

    coll.insert(true).many(objects.map(_._1)).map(_ => objects.map(_._2))
  }

  private def extractId(obj: JsObject) = {
    obj \ "_id" match {
      case JsDefined(JsObject(Seq("$oid" -> JsString(oid)))) =>
        BSONObjectID
          .parse(oid.getBytes)
          .getOrElse(sys.error(s"Could not parse _id field: $oid"))

      case JsDefined(JsString(oid)) =>
        BSONObjectID
          .parse(oid)
          .getOrElse(sys.error(s"Could not parse _id field: $oid"))
      case JsDefined(JsObject(values)) if values.contains("$oid") =>
        values
          .get("$oid")
          .fold(sys.error(
            s"Could not find $$oid in jsobject of _id field: $values")) {
            case JsString(oid) =>
              BSONObjectID
                .parse(oid)
                .getOrElse(sys.error(s"Could not parse _id field: $oid"))
            case value => sys.error(s"Could not parse _id field: $value")
          }
      case f => sys.error(s"Could not parse _id field: $f")
    }
  }

  def findAll(limit: Int = -1)(implicit
      dbSession: DBSession): Future[Iterable[(T, BSONObjectID)]] = {
    coll
      .find(BSONDocument(), Option.empty[BSONDocument])
      .cursor[JsObject]()
      .collect[Iterable](limit, Cursor.FailOnError())
      .map(_.map(js => (js.as[T], (js \ "_id" \ "$oid").as[BSONObjectID])))
  }

  protected[repositories] def find(sel: JsObject,
                                   limit: Int = -1,
                                   skip: Int = 0,
                                   sort: JsObject = Json.obj(),
                                   projection: JsObject = Json.obj())(implicit
      dbSession: DBSession): Future[Iterable[(T, BSONObjectID)]] = {
    coll
      .find(sel, Option.empty[JsObject])
      .projection(projection)
      .sort(ValueConverters.toDocument(sort))
      .skip(skip)
      .batchSize(limit)
      .cursor[JsObject]()
      .collect[Iterable](limit, Cursor.FailOnError())
      .map(_.map { js =>
        val id =
          (js \ "_id").as[BSONObjectID](BaseFormat.plainBSONObjectIDFormat)
        (js.as[T], id)
      })
  }

  def findFirst(sel: JsObject, skip: Int = 0)(implicit
      dbSession: DBSession): Future[Option[(T, BSONObjectID)]] = {
    find(sel, 1, skip).map(_.headOption)
  }

  def findById(id: ID)(implicit
      fact: ID => JsValueWrapper,
      dbSession: DBSession): Future[Option[T]] = {
    val sel = Json.obj("id" -> fact(id))
    find(sel).map(_.headOption.map(_._1))
  }

}
