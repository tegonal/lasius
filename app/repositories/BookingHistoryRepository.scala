package repositories

import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent._
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.json.BSONFormats._
import play.api.libs.json._
import models._
import play.api.Logger
import org.joda.time.DateTime
import repositories.MongoDBCommandSet._
import reactivemongo.core.commands.LastError
import akka.actor.Actor

trait BookingHistoryRepository extends BaseRepository[Booking, BookingId] with PersistentUserViewRepository[Booking, BookingId] {
  def findByUserIdAndRange(userId: UserId, from: DateTime, to: DateTime): Future[Traversable[Booking]]
}

class BookingHistoryMongoRepository extends BaseReactiveMongoRepository[Booking, BookingId] with BookingHistoryRepository
  with MongoPeristentUserViewRepository[Booking, BookingId] {
  def coll = db.collection[JSONCollection]("BookingHistory")

  def findByUserIdAndRange(userId: UserId, from: DateTime, to: DateTime): Future[Traversable[Booking]] = {
    val sel = Json.obj("userId" -> userId,
      And -> Json.arr(
        Json.obj("start" -> Json.obj(LowerOrEqualsThan -> to)),
        Json.obj("end" -> Json.obj(GreaterOrEqualsThan -> from))))
    Logger.debug(s"findByUserAndRange:$sel")
    find(sel) map (_.map(_._1))
  }

}