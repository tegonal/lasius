package repositories

import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent._
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.json.BSONFormats._
import play.api.libs.json._
import models._
import reactivemongo.core.commands.LastError
import play.api.Logger

trait BookingHistoryRepository extends BaseRepository[Booking, BookingId] {
  def deleteHistory(userId: UserId): Future[Boolean]
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
}