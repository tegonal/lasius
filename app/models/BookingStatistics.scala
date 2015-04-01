package models

import reactivemongo.bson.BSONObjectID
import models.BaseFormat._
import com.tegonal.play.json._
import play.api.libs.json._
import com.tegonal.play.json.TypedId._
import org.joda.time.DateTime
import org.joda.time.Duration
import org.joda.time.LocalDate

trait OperatorEntity[I <: BaseId[_], E] extends BaseEntity[I] {
  val duration: Duration
  val day: DateTime

  def +(that: E): E
  def -(that: E): E

  def duration(duration: Duration): E
}

case class BookingByProjectId(value: BSONObjectID = BSONObjectID.generate) extends BaseBSONObjectId

object BookingByProjectId {
  implicit val idFormat: Format[BookingByProjectId] = BaseFormat.idformat[BookingByProjectId](BookingByProjectId.apply _)
}

case class BookingByCategoryId(value: BSONObjectID = BSONObjectID.generate) extends BaseBSONObjectId

object BookingByCategoryId {
  implicit val idFormat: Format[BookingByCategoryId] = BaseFormat.idformat[BookingByCategoryId](BookingByCategoryId.apply _)
}

case class BookingByTagId(value: BSONObjectID = BSONObjectID.generate) extends BaseBSONObjectId

object BookingByTagId {
  implicit val idFormat: Format[BookingByTagId] = BaseFormat.idformat[BookingByTagId](BookingByTagId.apply _)
}

case class BookingByProject(id: BookingByProjectId, userId: UserId, day: DateTime, projectId: ProjectId, duration: Duration) extends OperatorEntity[BookingByProjectId, BookingByProject] {
  def +(that: BookingByProject): BookingByProject = {
    BookingByProject(id, userId, day, projectId, duration.plus(that.duration))
  }

  def -(that: BookingByProject): BookingByProject = {
    BookingByProject(id, userId, day, projectId, duration.minus(that.duration))
  }

  def duration(duration: Duration): BookingByProject = {
    copy(duration = duration)
  }
}

object BookingByProject {
  implicit val bookingByProjectFormat = Json.format[BookingByProject]
}

case class BookingByCategory(id: BookingByCategoryId, userId: UserId, day: DateTime, categoryId: CategoryId, duration: Duration) extends OperatorEntity[BookingByCategoryId, BookingByCategory] {
  def +(that: BookingByCategory): BookingByCategory = {
    BookingByCategory(id, userId, day, categoryId, duration.plus(that.duration))
  }

  def -(that: BookingByCategory): BookingByCategory = {
    BookingByCategory(id, userId, day, categoryId, duration.minus(that.duration))
  }

  def duration(duration: Duration): BookingByCategory = {
    copy(duration = duration)
  }
}

object BookingByCategory {
  implicit val bookingByCategoryFormat = Json.format[BookingByCategory]
}

case class BookingByTag(id: BookingByTagId, userId: UserId, day: DateTime, tagId: TagId, duration: Duration) extends OperatorEntity[BookingByTagId, BookingByTag] {
  def +(that: BookingByTag): BookingByTag = {
    BookingByTag(id, userId, day, tagId, duration.plus(that.duration))
  }

  def -(that: BookingByTag): BookingByTag = {
    BookingByTag(id, userId, day, tagId, duration.minus(that.duration))
  }

  def duration(duration: Duration): BookingByTag = {
    copy(duration = duration)
  }
}

object BookingByTag {
  implicit val bookingByTagFormat = Json.format[BookingByTag]
}

