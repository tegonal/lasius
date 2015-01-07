package dao

import play.modules.reactivemongo.json.BSONFormats._
import reactivemongo.bson.BSONObjectID
import reactivemongo.core.commands.LastError
import reactivemongo.core.protocol.Query
import reactivemongo.api.QueryOpts
import reactivemongo.core.commands.PipelineOperator
import reactivemongo.core.commands.Aggregate
import reactivemongo.bson.BSONDocument
import scala.concurrent.ExecutionContext
import play.api.libs.json._
import scala.concurrent.Future
import play.api.libs.iteratee.Enumerator
import play.api.Logger
import play.api.Play.current
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.ReactiveMongoPlugin
import com.tegonal.play.json.TypedId.BaseId
import models.BaseEntity
import play.api.libs.json.Json.JsValueWrapper

class MongoDBCommandException(msg: String) extends RuntimeException

trait BaseDAO[T <: BaseEntity[ID], ID <: BaseId[_]] {
  def coll: JSONCollection

  def get(id: BSONObjectID): Future[Option[(T, BSONObjectID)]]

  def insert(t: T)(implicit ctx: ExecutionContext): Future[BSONObjectID]

  def update(sel: JsObject, modifier: JsObject, upsert: Boolean = true)(implicit ctx: ExecutionContext): Future[Boolean]

  def update(doc: T)(implicit ctx: ExecutionContext): Future[Boolean]

  def find(sel: JsObject, limit: Int = 0, skip: Int = 0, sort: JsObject = Json.obj(), projection: JsObject = Json.obj())(implicit ctx: ExecutionContext): Future[Traversable[(T, BSONObjectID)]]

  def findStream(sel: JsObject, skip: Int = 0, pageSize: Int = 0)(implicit ctx: ExecutionContext): Enumerator[TraversableOnce[(T, BSONObjectID)]]

  def findByIds(ids: Traversable[BSONObjectID], limit: Int)(implicit ctx: ExecutionContext): Future[Traversable[(T, BSONObjectID)]]

  def findIds(sel: JsObject): Future[Seq[BSONObjectID]]

  def aggregate(pipelineOps: Seq[PipelineOperator]): Future[Stream[BSONDocument]]

  def findById(id: ID)(implicit fact: ID => JsValueWrapper): Future[Option[T]]

  def update(obj: T)(implicit fact: ID => JsValueWrapper): Future[LastError]
}

abstract class BaseReactiveMongoDAO[T <: BaseEntity[ID], ID <: BaseId[_]](implicit ctx: ExecutionContext, format: Format[T]) extends BaseDAO[T, ID] {

  lazy val db = ReactiveMongoPlugin.db

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
    coll.update(sel, modifier, upsert = upsert)
      .flatMap {
        _ match {
          case LastError(ok, _, _, _, _, _, _) => Future.successful(ok)
          case e => Future.failed(new MongoDBCommandException(e.errMsg.getOrElse("Update failed")))
        }
      }
  }

  def update(doc: T)(implicit ctx: ExecutionContext): Future[Boolean] = {
    coll.save(doc)
      .flatMap {
        _ match {
          case LastError(ok, _, _, _, _, _, _) => Future.successful(ok)
          case e => Future.failed(new MongoDBCommandException(e.errMsg.getOrElse("Update failed")))
        }
      }
  }

  def insert(t: T)(implicit ctx: ExecutionContext): Future[BSONObjectID] = {
    val id = BSONObjectID.generate
    val obj = format.writes(t).as[JsObject]
    obj \ "_id" match {
      case _: JsUndefined =>
        coll.insert(obj ++ Json.obj("_id" -> id))
          .map { _ => id }

      case JsObject(Seq((_, JsString(oid)))) =>
        coll.insert(obj).map { _ => BSONObjectID(oid) }

      case JsString(oid) =>
        coll.insert(obj).map { _ => BSONObjectID(oid) }

      case f => sys.error(s"Could not parse _id field: $f")
    }
  }

  def find(sel: JsObject, limit: Int = 0, skip: Int = 0, sort: JsObject = Json.obj(), projection: JsObject = Json.obj())(implicit ctx: ExecutionContext): Future[Traversable[(T, BSONObjectID)]] = {

    val cursor = coll.find(sel).projection(projection).sort(sort).options(QueryOpts().skip(skip).batchSize(limit)).cursor[JsObject]
    val l = if (limit != 0) cursor.collect[Traversable](limit) else cursor.collect[Traversable]()
    l.map(_.map(js => (js.as[T], (js \ "_id").as[BSONObjectID])))
  }

  def findStream(sel: JsObject, skip: Int = 0, pageSize: Int = 0)(implicit ctx: ExecutionContext): Enumerator[TraversableOnce[(T, BSONObjectID)]] = {
    val cursor = coll.find(sel).options(QueryOpts().skip(skip)).cursor[JsObject]
    val enum = if (pageSize != 0) cursor.enumerateBulks(pageSize) else cursor.enumerateBulks()
    enum.map(_.map(js => (js.as[T], (js \ "_id").as[BSONObjectID])))
  }

  def aggregate(pipelineOps: Seq[PipelineOperator]): Future[Stream[BSONDocument]] = {
    val cmd = Aggregate(coll.name, pipelineOps)
    coll.db.command(cmd)
  }

  def findById(id: ID)(implicit fact: ID => JsValueWrapper): Future[Option[T]] = {
    val sel = Json.obj("id" -> fact(id))
    find(sel) map (_.headOption.map(_._1))
  }

  def update(obj: T)(implicit fact: ID => JsValueWrapper): Future[LastError] = {
    val selector = Json.obj("id" -> fact(obj.id))
    val modifier = Json.obj("$set" -> obj)
    coll.update(selector, modifier)
  }
}