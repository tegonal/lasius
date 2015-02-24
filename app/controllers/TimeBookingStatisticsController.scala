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
import repositories.BookingStatisticRepository
import scala.concurrent.Future
import repositories.BookingByCategoryRepository

class TimeBookingStatisticsController {
  self: Controller with UserBookingStatisticsRepositoryComponent =>

  def getAggregatedStatistics(source: String, userId: UserId, from: DateTime, to: DateTime) = Action.async {
    Logger.debug(s"getAggregatedStatistics, source:$source, userId:$userId, from:$from, to:$to")
    source match {
      case "tag" => getAggregatedStatisticsByTag(userId, from, to)
      case "category" => getAggregatedStatisticsByCategory(userId, from, to)
      case "project" => getAggregatedStatisticsByProject(userId, from, to)
      case _ => Future.successful(BadRequest)
    }
  }

  def getAggregatedStatisticsByTag(userId: UserId, from: DateTime, to: DateTime) = {
    bookingByTagRepository.findByUserIdAndRange(userId, from, to) map { bookings =>
      val aggregatedMap = bookings.groupBy(_.tagId) map { entry =>
        (entry._1, entry._2.map(_.duration.getMillis).foldLeft(0l)((a, b) => a + b))
      }
      Ok(Json.toJson(aggregatedMap.map(entry => Json.obj("label" -> entry._1, "value" -> entry._2))))
    }
  }

  def getAggregatedStatisticsByProject(userId: UserId, from: DateTime, to: DateTime) = {
    bookingByProjectRepository.findByUserIdAndRange(userId, from, to) map { bookings =>
      val aggregatedMap = bookings.groupBy(_.projectId) map { entry =>
        (entry._1, entry._2.map(_.duration.getMillis).foldLeft(0l)((a, b) => a + b))
      }
      Ok(Json.toJson(aggregatedMap.map(entry => Json.obj("label" -> entry._1, "value" -> entry._2))))
    }
  }

  def getAggregatedStatisticsByCategory(userId: UserId, from: DateTime, to: DateTime) = {
    bookingByCategoryRepository.findByUserIdAndRange(userId, from, to) map { bookings =>
      val aggregatedMap = bookings.groupBy(_.categoryId) map { entry =>
        (entry._1, entry._2.map(_.duration.getMillis).foldLeft(0l)((a, b) => a + b))
      }
      Ok(Json.toJson(aggregatedMap.map(entry => Json.obj("label" -> entry._1, "value" -> entry._2))))
    }
  }

  def getStatistics(source: String, userId: UserId, from: DateTime, to: DateTime) = Action.async {
    Logger.debug(s"getStatistics, source:$source, userId:$userId, from:$from, to:$to")
    val days = Days.daysBetween(from, to)
    val range = for {
      diff <- 0 to days.getDays
    } yield {
      from.plusDays(diff)
    }
    source match {
      case "tag" => getStatisticsByTag(userId, from, to, range)
      case "category" => getStatisticsByCategory(userId, from, to, range)
      case "project" => getStatisticsByProject(userId, from, to, range)
      case _ => Future.successful(BadRequest)
    }
  }

  private def toJson(day: DateTime, millis: Long) = {
    Json.obj("day" -> Json.toJson(day), "duration" -> Json.toJson(millis))
  }

  def getStatisticsByTag(userId: UserId, from: DateTime, to: DateTime, range: Seq[DateTime]) = {
    bookingByTagRepository.findByUserIdAndRange(userId, from, to) map { bookings =>
      val aggregatedMap = bookings.groupBy(_.tagId) map { entry =>
        (entry._1, range.map(r => entry._2.filter(_.day == r).headOption.map(b => toJson(b.day, b.duration.getMillis)).getOrElse(toJson(r, 0))))
      }
      Ok(Json.toJson(aggregatedMap.map(entry => Json.obj("key" -> entry._1, "values" -> entry._2))))
    }
  }

  def getStatisticsByCategory(userId: UserId, from: DateTime, to: DateTime, range: Seq[DateTime]) = {
    bookingByCategoryRepository.findByUserIdAndRange(userId, from, to) map { bookings =>
      val aggregatedMap = bookings.groupBy(_.categoryId) map { entry =>
        (entry._1, range.map(r => entry._2.filter(_.day == r).headOption.map(b => toJson(b.day, b.duration.getMillis)).getOrElse(toJson(r, 0))))
      }
      Ok(Json.toJson(aggregatedMap.map(entry => Json.obj("key" -> entry._1, "values" -> entry._2))))
    }
  }

  def getStatisticsByProject(userId: UserId, from: DateTime, to: DateTime, range: Seq[DateTime]) = {
    bookingByProjectRepository.findByUserIdAndRange(userId, from, to) map { bookings =>
      val aggregatedMap = bookings.groupBy(_.projectId) map { entry =>
        (entry._1, range.map(r => entry._2.filter(_.day == r).headOption.map(b => toJson(b.day, b.duration.getMillis)).getOrElse(toJson(r, 0))))
      }
      Ok(Json.toJson(aggregatedMap.map(entry => Json.obj("key" -> entry._1, "values" -> entry._2))))
    }
  }
}

object TimeBookingStatisticsController extends TimeBookingStatisticsController with Controller with MongoUserBookingStatisticsRepositoryComponent