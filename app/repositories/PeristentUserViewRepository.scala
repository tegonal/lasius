package repositories

import models._
import play.api.libs.json.Format
import scala.concurrent.ExecutionContext
import com.tegonal.play.json.TypedId.BaseId
import scala.concurrent.Future
import org.joda.time.DateTime
import play.api.libs.json._
import reactivemongo.core.commands._
import repositories.MongoDBCommandSet._
import play.api.Logger

trait PersistentUserViewRepository[T <: BaseEntity[ID], ID <: BaseId[_]] {
  def deleteByUser(userId: UserId)(implicit ctx: ExecutionContext, format: Format[T]): Future[Boolean]

  def findByUserIdAndRange(userId: UserId, from: DateTime, to: DateTime)(implicit ctx: ExecutionContext, format: Format[T]): Future[Traversable[T]]
}

trait MongoPeristentUserViewRepository[T <: BaseEntity[ID], ID <: BaseId[_]] extends PersistentUserViewRepository[T, ID] {
  self: BaseReactiveMongoRepository[T, ID] with BaseRepository[T, ID] =>

  def deleteByUser(userId: UserId)(implicit ctx: ExecutionContext, format: Format[T]): Future[Boolean] = {
    val sel = Json.obj("userId" -> userId)
    coll.remove(sel) map {
      _ match {
        case LastError(ok, _, _, _, _, _, _) => ok
        case e => false
      }
    }
  }

  def findByUserIdAndRange(userId: UserId, from: DateTime, to: DateTime)(implicit ctx: ExecutionContext, format: Format[T]): Future[Traversable[T]] = {
    val sel = Json.obj("userId" -> userId,
      And -> Json.arr(
        Json.obj("start" -> Json.obj(LowerOrEqualsThan -> to)),
        Json.obj("end" -> Json.obj(GreaterOrEqualsThan -> from))))
    Logger.debug(s"findByUserAndRange:$sel")
    find(sel) map (_.map(_._1))
  }

}