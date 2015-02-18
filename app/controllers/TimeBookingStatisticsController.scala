package controllers

import play.api.mvc.Controller
import repositories.UserBookingStatisticsRepositoryComponent
import models._
import play.api.mvc.Action
import play.api.Logger
import org.joda.time._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import repositories.MongoUserBookingStatisticsRepositoryComponent

class TimeBookingStatisticsController {
  self: Controller with UserBookingStatisticsRepositoryComponent =>

  def getAggregatedStatisticsByTagAndRange(userId: UserId, from: DateTime, to: DateTime) = Action.async {
    Logger.debug(s"getAggregatedStatisticsByTagAndRange, userId:$userId, from:$from, to:$to")

    bookingByTagRepository.findByUserIdAndRange(userId, from, to) map { bookings =>
      val aggregatedMap = bookings.groupBy(_.tagId) map { entry =>
        (entry._1, entry._2.map(_.duration.getMillis).foldLeft(0l)((a, b) => a + b))
      }
      Ok(Json.toJson(aggregatedMap.map(entry => Json.obj("label" -> entry._1, "value" -> entry._2))))
    }
  }
}

object TimeBookingStatisticsController extends TimeBookingStatisticsController with Controller with MongoUserBookingStatisticsRepositoryComponent