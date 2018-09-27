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

import play.api.mvc.Controller
import repositories._
import org.joda.time._
import play.api.mvc.Action
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.Logger
import models._
import utils.StringUtils._
import org.joda.time.format.DateTimeFormat
import java.text.DecimalFormat
import play.api.mvc.Result

object CSVHelper {
  val CSV_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss"

  def format(date: DateTime) =
    DateTimeFormat.forPattern(CSV_DATETIME_FORMAT).print(date)

  def timeDiff(from: DateTime, to: DateTime) = {
    val minutes: Double = Minutes.minutesBetween(from, to).getMinutes()
    new DecimalFormat("0.00").format(minutes / 60);
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
    import controllers.CSVHelper._

    bookingHistoryRepository.findByUserIdAndRange(Some(subject.userId), from, to) map ( bookingsToCSV )
    }
  }

  def exportTimeBookingHistoryByRange(from: DateTime, to: DateTime) = HasRole(FreeUser, parse.empty) { implicit subject => implicit request => {
    import controllers.CSVHelper._

    bookingHistoryRepository.findByUserIdAndRange(None, from, to) map ( bookingsToCSV )
  }
  }

  private def bookingsToCSV: Traversable[Booking] => Result = { bookings =>
    import controllers.CSVHelper._
    val headers = Seq("User", "Category", "Project", "Tags", "Start", "End", "Comment", "Amount").mkString(",")
    val content = bookings.map(_.toCSV).mkString("\n");
    val csv = headers + "\n" + content;

    Ok(csv).withHeaders(
      "Content-Type" -> "text/csv",
      "Content-Disposition" -> s"attachment; filename=export.csv")
  }
}

object TimeBookingHistoryController extends TimeBookingHistoryController with Controller with MongoUserBookingHistoryRepositoryComponent with Security with DefaultSecurityComponent {
}