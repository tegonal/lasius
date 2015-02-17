package models

import reactivemongo.bson.BSONObjectID

import models.BaseFormat._
import com.tegonal.play.json._
import play.api.libs.json._
import com.tegonal.play.json.TypedId._
import org.joda.time.DateTime
import org.joda.time.Duration
import org.joda.time.DateMidnight
import org.bson.BSONObject

case class BookingByProjectId(value: (DateMidnight, ProjectId)) extends CompositeBaseId[DateMidnight, ProjectId]

object BookingByProjectId {
  implicit val idFormat: Format[BookingByProjectId] = BaseFormat.idformat[BookingByProjectId, DateMidnight, ProjectId](BookingByProjectId.apply _, BaseFormat.dateMidnightFormat, ProjectId.idFormat)
}

case class BookingByCategoryId(value: (DateMidnight, CategoryId)) extends CompositeBaseId[DateMidnight, CategoryId]

object BookingByCategoryId {
  implicit val idFormat: Format[BookingByCategoryId] = BaseFormat.idformat[BookingByCategoryId, DateMidnight, CategoryId](BookingByCategoryId.apply _, BaseFormat.dateMidnightFormat, CategoryId.idFormat)
}

case class BookingByTagId(value: (DateMidnight, TagId)) extends CompositeBaseId[DateMidnight, TagId]

object BookingByTagId {
  implicit val idFormat: Format[BookingByTagId] = BaseFormat.idformat[BookingByTagId, DateMidnight, TagId](BookingByTagId.apply _, BaseFormat.dateMidnightFormat, TagId.idFormat)
}

case class BookingByProject(id: BookingByProjectId, duration: Duration) extends BaseEntity[BookingByProjectId] {
}

object BookingByProject {
  implicit val bookingByProjectFormat = Json.format[BookingByProject]
}

case class BookingByCategory(id: BookingByCategoryId, duration: Duration) extends BaseEntity[BookingByCategoryId] {
}

object BookingByCategory {
  implicit val bookingByCategoryFormat = Json.format[BookingByCategory]
}

case class BookingByTag(id: BookingByTagId, duration: Duration) extends BaseEntity[BookingByTagId] {
}

object BookingByTag {
  implicit val bookingByTagFormat = Json.format[BookingByTag]
}

