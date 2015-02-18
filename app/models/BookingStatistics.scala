package models

import reactivemongo.bson.BSONObjectID
import models.BaseFormat._
import com.tegonal.play.json._
import play.api.libs.json._
import com.tegonal.play.json.TypedId._
import org.joda.time.DateTime
import org.joda.time.Duration
import org.bson.BSONObject
import org.joda.time.LocalDate

trait OperatorEntity[I <: BaseId[_], E] extends BaseEntity[I] {
  def +(that: E): E
  def -(that: E): E
}

case class BookingByProjectId(value: (LocalDate, ProjectId)) extends CompositeBaseId[LocalDate, ProjectId]

object BookingByProjectId {
  implicit val idFormat: Format[BookingByProjectId] = BaseFormat.idformat[BookingByProjectId, LocalDate, ProjectId](BookingByProjectId.apply _, BaseFormat.localDateFormat, ProjectId.idFormat)
}

case class BookingByCategoryId(value: (LocalDate, CategoryId)) extends CompositeBaseId[LocalDate, CategoryId]

object BookingByCategoryId {
  implicit val idFormat: Format[BookingByCategoryId] = BaseFormat.idformat[BookingByCategoryId, LocalDate, CategoryId](BookingByCategoryId.apply _, BaseFormat.localDateFormat, CategoryId.idFormat)
}

case class BookingByTagId(value: (LocalDate, TagId)) extends CompositeBaseId[LocalDate, TagId]

object BookingByTagId {
  implicit val idFormat: Format[BookingByTagId] = BaseFormat.idformat[BookingByTagId, LocalDate, TagId](BookingByTagId.apply _, BaseFormat.localDateFormat, TagId.idFormat)
}

case class BookingByProject(id: BookingByProjectId, duration: Duration) extends OperatorEntity[BookingByProjectId, BookingByProject] {
  def +(that: BookingByProject): BookingByProject = {
    BookingByProject(id, duration.plus(that.duration))
  }

  def -(that: BookingByProject): BookingByProject = {
    BookingByProject(id, duration.minus(that.duration))
  }
}

object BookingByProject {
  implicit val bookingByProjectFormat = Json.format[BookingByProject]
}

case class BookingByCategory(id: BookingByCategoryId, duration: Duration) extends OperatorEntity[BookingByCategoryId, BookingByCategory] {
  def +(that: BookingByCategory): BookingByCategory = {
    BookingByCategory(id, duration.plus(that.duration))
  }

  def -(that: BookingByCategory): BookingByCategory = {
    BookingByCategory(id, duration.minus(that.duration))
  }
}

object BookingByCategory {
  implicit val bookingByCategoryFormat = Json.format[BookingByCategory]
}

case class BookingByTag(id: BookingByTagId, duration: Duration) extends OperatorEntity[BookingByTagId, BookingByTag] {
  def +(that: BookingByTag): BookingByTag = {
    BookingByTag(id, duration.plus(that.duration))
  }

  def -(that: BookingByTag): BookingByTag = {
    BookingByTag(id, duration.minus(that.duration))
  }
}

object BookingByTag {
  implicit val bookingByTagFormat = Json.format[BookingByTag]
}

