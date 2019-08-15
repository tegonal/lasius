/*   __                          __                                          *\
*   / /____ ___ ____  ___  ___ _/ /       lasius                      *
*  / __/ -_) _ `/ _ \/ _ \/ _ `/ /        contributed by tegonal              *
*  \__/\__/\_, /\___/_//_/\_,_/_/         http://tegonal.com/                 *
*         /___/                                                               *
*                                                                             *
* This program is free software: you can redistribute it and/or modify it     *
* under the terms of the GNU General Public License as published by    *
* the Free Software Foundation, either version 3 of the License,              *
* or (at your option) any later version.                                      *
*                                                                             *
* This program is distributed in the hope that it will be useful, but         *
* WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY  *
* or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for *
* more details.                                                               *
*                                                                             *
* You should have received a copy of the GNU General Public License along     *
* with this program. If not, see http://www.gnu.org/licenses/                 *
*                                                                             *
\*                                                                           */
package models

import com.tegonal.play.json.TypedId._
import models.BaseFormat._
import org.joda.time.{DateTime, Duration}
import play.api.libs.json._
import reactivemongo.bson.BSONObjectID

trait OperatorEntity[I <: BaseId[_], E] extends BaseEntity[I] {
  val duration: Duration
  val day: DateTime

  def invert: E

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

case class BookingByProject(_id: BookingByProjectId, userId: UserId, day: DateTime, projectId: ProjectId, duration: Duration) extends OperatorEntity[BookingByProjectId, BookingByProject] {
  val id = _id

  def invert: BookingByProject = {
    BookingByProject(id, userId, day, projectId, Duration.ZERO.minus(duration))
  }

  def duration(duration: Duration): BookingByProject = {
    copy(duration = duration)
  }
}

object BookingByProject {
  implicit val bookingByProjectFormat = Json.format[BookingByProject]
}

case class BookingByCategory(_id: BookingByCategoryId, userId: UserId, day: DateTime, categoryId: CategoryId, duration: Duration) extends OperatorEntity[BookingByCategoryId, BookingByCategory] {
  val id = _id

  def invert: BookingByCategory = {
    BookingByCategory(id, userId, day, categoryId, Duration.ZERO.minus(duration))
  }

  def duration(duration: Duration): BookingByCategory = {
    copy(duration = duration)
  }
}

object BookingByCategory {
  implicit val bookingByCategoryFormat = Json.format[BookingByCategory]
}

case class BookingByTag(_id: BookingByTagId, userId: UserId, day: DateTime, tagId: TagId, duration: Duration) extends OperatorEntity[BookingByTagId, BookingByTag] {
  val id = _id

  def duration(duration: Duration): BookingByTag = {
    copy(duration = duration)
  }

  def invert: BookingByTag = {
    BookingByTag(id, userId, day, tagId, Duration.ZERO.minus(duration))
  }
}

object BookingByTag {
  implicit val bookingByTagFormat = Json.format[BookingByTag]
}

