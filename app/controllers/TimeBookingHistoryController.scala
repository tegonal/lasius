package controllers

import play.api.mvc.Controller
import repositories.UserBookingHistoryRepositoryComponent
import org.joda.time.DateTime
import models.UserId
import repositories.MongoUserBookingHistoryRepositoryComponent
import play.api.mvc.Action
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.Logger

class TimeBookingHistoryController {
  self: Controller with UserBookingHistoryRepositoryComponent =>
  def getTimeBookingHistory(userId: UserId, from: DateTime, to: DateTime) = Action.async {
    Logger.debug(s"getTimeBookingHistory, userId:$userId, from:$from, to:$to")
    bookingHistoryRepository.findByUserIdAndRange(userId, from, to) map { bookings =>
      Ok(Json.toJson(bookings))
    }
  }
}

object TimeBookingHistoryController extends TimeBookingHistoryController with Controller with MongoUserBookingHistoryRepositoryComponent