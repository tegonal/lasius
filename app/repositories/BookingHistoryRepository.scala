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

trait BookingHistoryRepository extends BaseRepository[Booking, BookingId] {
  def deleteHistory(userId: UserId): Future[Boolean]

  def findByUserIdAndRange(userId: UserId, from: DateTime, to: DateTime): Future[Traversable[Booking]]
}

class BookingHistoryMongoRepository extends BaseReactiveMongoRepository[Booking, BookingId] with BookingHistoryRepository {
  def coll = db.collection[JSONCollection]("BookingHistory")

  def deleteHistory(userId: UserId): Future[Boolean] = {
    val sel = Json.obj("userId" -> userId)
    find(sel) flatMap { bookings =>
      Future.sequence(bookings map {
        case (booking, id) =>
          coll.remove(booking)
      }) map { results =>
        results.filter(!_.ok).size == 0
      }
    }
  }

  def findByUserIdAndRange(userId: UserId, from: DateTime, to: DateTime): Future[Traversable[Booking]] = {
    val sel = Json.obj("userId" -> userId,
      And -> Json.arr(Json.obj("start" -> Json.obj(GreaterOrEqualsThan -> from)),
        Json.obj("start" -> Json.obj(LowerOrEqualsThan -> to))))
    Logger.debug(s"findByUserAndRange:$sel")
    find(sel) map (_.map(_._1))
  }
}