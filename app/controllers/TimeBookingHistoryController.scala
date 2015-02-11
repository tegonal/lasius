package controllers

import play.api.mvc.Controller
import repositories.UserBookingHistoryRepositoryComponent
import org.joda.time.DateTime
import models.UserId
import repositories.MongoUserBookingHistoryRepositoryComponent
import play.api.mvc.Action
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

class TimeBookingHistoryController {
  self: Controller with UserBookingHistoryRepositoryComponent =>
  def getTimeBookingHistory(userId: UserId, from: DateTime, to: DateTime) = Action.async {
    bookingHistoryRepository.findByUserIdAndRange(userId, from, to) map { bookings =>
      Ok(Json.obj("bookings" -> bookings))
    }
  }
}

object TimeBookingHistoryController extends TimeBookingHistoryController with Controller with MongoUserBookingHistoryRepositoryComponent