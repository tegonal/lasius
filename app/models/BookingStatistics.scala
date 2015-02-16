package models

import reactivemongo.bson.BSONObjectID
import models.BaseFormat._
import com.tegonal.play.json._
import play.api.libs.json._
import com.tegonal.play.json.TypedId._
import org.joda.time.DateTime
import org.joda.time.Duration
import org.joda.time.DateMidnight

case class BookingByProjectId(value: BSONObjectID) extends BaseBSONObjectId

object BookingByProjectId {
  implicit val idFormat: Format[BookingByProjectId] = BaseFormat.idformat[BookingByProjectId](BookingByProjectId.apply _)
}

case class BookingByCategoryId(value: BSONObjectID) extends BaseBSONObjectId

object BookingByCategoryId {
  implicit val idFormat: Format[BookingByCategoryId] = BaseFormat.idformat[BookingByCategoryId](BookingByCategoryId.apply _)
}

case class BookingByTagId(value: BSONObjectID) extends BaseBSONObjectId

object BookingByTagId {
  implicit val idFormat: Format[BookingByTagId] = BaseFormat.idformat[BookingByTagId](BookingByTagId.apply _)
}

case class BookingByProject(id: BookingByProjectId, day: DateMidnight, projectId: ProjectId, duration: Duration) extends BaseEntity[BookingByProjectId] {
}

object BookingByProject {
  implicit val bookingByProjectFormat = Json.format[BookingByProject]
}

case class BookingByCategory(id: BookingByCategoryId, day: DateMidnight, categoryId: CategoryId, duration: Duration) extends BaseEntity[BookingByCategoryId] {
}

object BookingByCategory {
  implicit val bookingByCategoryFormat = Json.format[BookingByCategory]
}

case class BookingByTag(id: BookingByTagId, day: DateMidnight, tagId: TagId, duration: Duration) extends BaseEntity[BookingByTagId] {
}

object BookingByTag {
  implicit val bookingByTagFormat = Json.format[BookingByTag]
}

