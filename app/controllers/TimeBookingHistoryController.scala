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
import models.FreeUser

class TimeBookingHistoryController {
  self: Controller with UserBookingHistoryRepositoryComponent with Security =>
  def getTimeBookingHistory(from: DateTime, to: DateTime) = HasRole(FreeUser, parse.empty) {
    implicit subject =>
      implicit request => {
        Logger.debug(s"getTimeBookingHistory, userId:$subject.userId, from:$from, to:$to")
        bookingHistoryRepository.findByUserIdAndRange(subject.userId, from, to) map { bookings =>
          Ok(Json.toJson(bookings))
        }
      }
  }
}

object TimeBookingHistoryController extends TimeBookingHistoryController with Controller with MongoUserBookingHistoryRepositoryComponent with Security with DefaultSecurityComponent