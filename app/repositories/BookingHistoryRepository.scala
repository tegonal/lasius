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

trait BookingHistoryRepository extends BaseRepository[Booking, BookingId] with PersistentUserViewRepository[Booking, BookingId] {
}

class BookingHistoryMongoRepository extends BaseReactiveMongoRepository[Booking, BookingId] with BookingHistoryRepository
  with MongoPeristentUserViewRepository[Booking, BookingId] {
  def coll = db.collection[JSONCollection]("BookingHistory")
}