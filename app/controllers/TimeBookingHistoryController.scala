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
package controllers

import java.text.DecimalFormat

import models._
import org.joda.time._
import org.joda.time.format.DateTimeFormat
import concurrent.ExecutionContext.Implicits.global
import play.api.libs.json._
import play.api.Logger
import play.api.mvc.{Controller, Result}
import repositories._
import utils.StringUtils._

object CSVHelper {
  val CSV_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss"

  def format(date: DateTime) =
    DateTimeFormat.forPattern(CSV_DATETIME_FORMAT).print(date)

  def timeDiff(from: DateTime, to: DateTime) = {
    val diff = to.getMillis - from.getMillis
    new DecimalFormat("0.00").format(diff / 3600000.0);
  }

  implicit class CSVBookingWrapper(val booking: Booking) {

    def toCSV = Seq(
      booking.userId.value.quote,
      booking.projectId.value.quote,
      booking.categoryId.value.quote,
      booking.tags.toCSV(t => t.value).quote,
      format(booking.start),
      booking.end.map(format(_)).getOrElse(""),
      booking.comment.quote,
      timeDiff(booking.start, booking.end.get).quote).mkString(",")
  }

  implicit class CSVSeqWrapper[A](val seq: Seq[A]) {
    def toCSV(f: A => String) = seq.map(t => f(t)).mkString(",")
  }
}

class TimeBookingHistoryController {
  self: Controller with UserBookingHistoryRepositoryComponent with Security =>
  def getTimeBookingHistoryByRange(from: DateTime, to: DateTime) = HasRole(FreeUser, parse.empty) { implicit subject => implicit request => {
    Logger.debug(s"getTimeBookingHistory from:$from, to:$to")
    bookingHistoryRepository.findByUserIdAndRange(None, from, to) map { bookings =>
      Ok(Json.toJson(bookings))
    }
  }
  }

  def getTimeBookingHistory(from: DateTime, to: DateTime) = HasRole(FreeUser, parse.empty) { implicit subject => implicit request => {
    Logger.debug(s"getTimeBookingHistory, userId:$subject.userId, from:$from, to:$to")
    bookingHistoryRepository.findByUserIdAndRange(Some(subject.userId), from, to) map { bookings =>
      Ok(Json.toJson(bookings))
    }
  }
  }

  def exportTimeBookingHistory(from: DateTime, to: DateTime) = HasRole(FreeUser, parse.empty) { implicit subject => implicit request => {

    bookingHistoryRepository.findByUserIdAndRange(Some(subject.userId), from, to) map ( bookingsToCSV )
    }
  }

  def exportTimeBookingHistoryByRange(from: DateTime, to: DateTime) = HasRole(FreeUser, parse.empty) { implicit subject => implicit request => {

    bookingHistoryRepository.findByUserIdAndRange(None, from, to) map ( bookingsToCSV )
  }
  }

  private def bookingsToCSV: Traversable[Booking] => Result = { bookings =>
    import controllers.CSVHelper._
    val headers = Seq("User", "Category", "Project", "Tags", "Start", "End", "Comment", "Amount").mkString(",")
    val content = bookings.map(_.toCSV).mkString("\n")
    val csv = headers + "\n" + content

    Ok(csv).withHeaders(
      "Content-Type" -> "text/csv",
      "Content-Disposition" -> s"attachment; filename=export.csv")
  }
}

object TimeBookingHistoryController extends TimeBookingHistoryController with Controller with MongoUserBookingHistoryRepositoryComponent with Security with DefaultSecurityComponent with DefaultCacheProvider {
}