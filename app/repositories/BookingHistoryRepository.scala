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
import akka.actor.Actor

trait BookingHistoryRepository extends BaseRepository[Booking, BookingId] with PersistentUserViewRepository[Booking, BookingId] {
  def findByUserIdAndRange(userId: Option[UserId], from: DateTime, to: DateTime): Future[Traversable[Booking]]

  def updateTimeBooking(bookingId: BookingId, from: DateTime, to: DateTime): Future[Boolean]

}

class BookingHistoryMongoRepository extends BaseReactiveMongoRepository[Booking, BookingId] with BookingHistoryRepository
  with MongoPeristentUserViewRepository[Booking, BookingId] {
  def coll = db.collection[JSONCollection]("BookingHistory")

  def findByUserIdAndRange(userId: Option[UserId], from: DateTime, to: DateTime): Future[Traversable[Booking]] = {
    val startEnd = Json.arr(
      Json.obj("start" -> Json.obj(LowerOrEqualsThan -> to)),
      Json.obj("end" -> Json.obj(GreaterOrEqualsThan -> from)))
    val sel = userId map { id =>
      Json.obj(
        "userId" -> id,
        And -> startEnd)
    } getOrElse {
      Json.obj(
        "userId" -> Json.obj("$exists" -> true, "$ne" -> JsNull),
        And -> startEnd)
    }

    Logger.debug(s"findByUserAndRange:$sel")
    find(sel) map (_.map(_._1))
  }

  def updateTimeBooking(bookingId: BookingId, from: DateTime, to: DateTime): Future[Boolean] = {
    Logger.debug(s"updateTimeBooking[$bookingId]: $from - $to")
    update(Json.obj("id" -> bookingId), Json.obj(Set -> Json.obj("start" -> from, "end" -> to)))
  }

}