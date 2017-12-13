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

  def invert: E

  def duration(duration: Duration): E
}

case class BookingByTagGroupId(value: BSONObjectID = BSONObjectID.generate) extends BaseBSONObjectId

object BookingByTagGroupId {
  implicit val idFormat: Format[BookingByTagGroupId] = BaseFormat.idformat[BookingByTagGroupId](BookingByTagGroupId.apply _)
}

case class BookingByTagId(value: BSONObjectID = BSONObjectID.generate) extends BaseBSONObjectId

object BookingByTagId {
  implicit val idFormat: Format[BookingByTagId] = BaseFormat.idformat[BookingByTagId](BookingByTagId.apply _)
}

case class BookingByTagGroup(_id: BookingByTagGroupId, userId: UserId, day: DateTime, tagGroupId: TagGroupId, duration: Duration) extends OperatorEntity[BookingByTagGroupId, BookingByTagGroup] {
  val id = _id

  def invert: BookingByTagGroup = {
    BookingByTagGroup(id, userId, day, tagGroupId, Duration.ZERO.minus(duration))
  }

  def duration(duration: Duration): BookingByTagGroup = {
    copy(duration = duration)
  }
}

object BookingByTagGroup {
  implicit val bookingByTagGroupFormat = Json.format[BookingByTagGroup]
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

