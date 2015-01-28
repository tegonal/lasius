package models

import reactivemongo.bson.BSONObjectID

import models.BaseFormat._
import com.tegonal.play.json._
import play.api.libs.json._
import com.tegonal.play.json.TypedId._
import org.joda.time.DateTime

case class BookingId(value: String) extends StringBaseId

object BookingId {
  implicit val idFormat: Format[BookingId] = Json.idformat[BookingId](BookingId.apply _)
}

@SerialVersionUID(1241414)
case class Booking(id: BookingId, start: DateTime, end: Option[DateTime], userId: UserId, projectId: ProjectId, tags: Seq[TagId]) extends BaseEntity[BookingId] {
  def duration = DateTime.now.minus(start.getMillis)
}

object Booking {
  implicit val bookingFormat = Json.format[Booking]
}