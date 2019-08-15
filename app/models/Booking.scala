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
import org.joda.time.DateTime
import play.api.libs.json._
import BaseFormat._

case class BookingId(value: String) extends StringBaseId

object BookingId {
  implicit val idFormat: Format[BookingId] = Json.idformat[BookingId](BookingId.apply _)
}

@SerialVersionUID(1241414)
case class BookingStub(categoryId: CategoryId, projectId: ProjectId, tags: Seq[TagId])

object BookingStub {
  implicit val bookingStubFormat: Format[BookingStub] = Json.format[BookingStub]
}

@SerialVersionUID(1241414)
case class Booking(id: BookingId, start: DateTime, end: Option[DateTime], userId: UserId, categoryId: CategoryId, projectId: ProjectId, tags: Seq[TagId], comment: Option[String] = None) extends BaseEntity[BookingId] {

  def createStub: BookingStub = {
    BookingStub(categoryId, projectId, tags)
  }
}

object Booking {
  implicit val bookingFormat = Json.format[Booking]
}