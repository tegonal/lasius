package dao

import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent._
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.json.BSONFormats._
import play.api.libs.json._
import models._
import org.joda.time.DateTime

trait BookingDAO extends BaseDAO[Booking, BookingId] {

  def findBooking(userId: UserId, projectId: ProjectId, start: DateTime): Future[Option[Booking]]
}

class BookingMongoDAO extends BaseReactiveMongoDAO[Booking, BookingId] with BookingDAO {
  def coll = db.collection[JSONCollection]("Booking")

  def findBooking(userId: UserId, projectId: ProjectId, start: DateTime): Future[Option[Booking]] = {
    val sel = Json.obj("projectId" -> projectId, "userId" -> userId)
    find(sel) map (_.headOption map (_._1))
  }
}