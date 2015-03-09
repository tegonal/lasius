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
import utils.DateTimeUtils._

class TimeBookingStatisticsController {
  self: Controller with UserBookingStatisticsRepositoryComponent with Security =>

  def getAggregatedStatistics(source: String, from: DateTime, to: DateTime) = HasRole(FreeUser, parse.empty) {
    implicit subject =>
      implicit request => {
        Logger.debug(s"getAggregatedStatistics, source:$source, userId:${subject.userId}, from:$from, to:$to")
        source match {
          case "tag" => getAggregatedStatisticsByTag(from, to)
          case "category" => getAggregatedStatisticsByCategory(from, to)
          case "project" => getAggregatedStatisticsByProject(from, to)
          case _ => Future.successful(BadRequest)
        }
      }
  }

  def getAggregatedStatisticsByTag(from: DateTime, to: DateTime)(implicit subject: Subject) = {
    bookingByTagRepository.findByUserIdAndRange(subject.userId, from, to) map { bookings =>
      val aggregatedMap = bookings.groupBy(_.tagId) map { entry =>
        (entry._1, entry._2.map(_.duration.getMillis).foldLeft(0l)((a, b) => a + b))
      }
      val sortedList = aggregatedMap.toList.sortBy(-_._2)
      Ok(Json.toJson(sortedList.map(entry => Json.obj("label" -> entry._1, "value" -> entry._2))))
    }
  }

  def getAggregatedStatisticsByProject(from: DateTime, to: DateTime)(implicit subject: Subject) = {
    bookingByProjectRepository.findByUserIdAndRange(subject.userId, from, to) map { bookings =>
      val aggregatedMap = bookings.groupBy(_.projectId) map { entry =>
        (entry._1, entry._2.map(_.duration.getMillis).foldLeft(0l)((a, b) => a + b))
      }
      val sortedList = aggregatedMap.toList.sortBy(-_._2)
      Ok(Json.toJson(sortedList.map(entry => Json.obj("label" -> entry._1, "value" -> entry._2))))
    }
  }

  def getAggregatedStatisticsByCategory(from: DateTime, to: DateTime)(implicit subject: Subject) = {
    bookingByCategoryRepository.findByUserIdAndRange(subject.userId, from, to) map { bookings =>
      val aggregatedMap = bookings.groupBy(_.categoryId) map { entry =>
        (entry._1, entry._2.map(_.duration.getMillis).foldLeft(0l)((a, b) => a + b))
      }
      val sortedList = aggregatedMap.toList.sortBy(-_._2)
      Ok(Json.toJson(sortedList.map(entry => Json.obj("label" -> entry._1, "value" -> entry._2))))
    }
  }

  def getStatistics(source: String, from: DateTime, to: DateTime) = HasRole(FreeUser, parse.empty) {
    implicit subject =>
      implicit request => {
        Logger.debug(s"getStatistics, source:$source, userId:${subject.userId}, from:$from, to:$to")
        val days = Days.daysBetween(from, to)
        val range = for {
          diff <- 0 to days.getDays
        } yield {
          from.plusDays(diff)
        }
        source match {
          case "tag" => getStatisticsByTag(from, to, range)
          case "category" => getStatisticsByCategory(from, to, range)
          case "project" => getStatisticsByProject(from, to, range)
          case _ => Future.successful(BadRequest)
        }
      }
  }

  private def toJson(day: DateTime, millis: Long) = {
    Json.obj("day" -> Json.toJson(day), "duration" -> Json.toJson(millis))
  }

  def getStatisticsByTag(from: DateTime, to: DateTime, range: Seq[DateTime])(implicit subject: Subject) = {
    bookingByTagRepository.findByUserIdAndRange(subject.userId, from, to) map { bookings =>
      val combinedMap = bookings.groupBy(_.tagId) map { entry =>
        (entry._1, range.map { r =>
          entry._2.filter(_.day.withTimeAtStartOfDay.toLocalDate.isEqual(r.withTimeAtStartOfDay.toLocalDate)).headOption.map { b =>
            toJson(r, b.duration.getMillis)
          }.getOrElse(toJson(r, 0))
        })
      }
      Ok(Json.toJson(combinedMap.map(entry => Json.obj("key" -> entry._1, "values" -> entry._2))))
    }
  }

  def getStatisticsByCategory(from: DateTime, to: DateTime, range: Seq[DateTime])(implicit subject: Subject) = {
    bookingByCategoryRepository.findByUserIdAndRange(subject.userId, from, to) map { bookings =>
      val combinedMap = bookings.groupBy(_.categoryId) map { entry =>
        (entry._1, range.map { r =>
          entry._2.filter(_.day.withTimeAtStartOfDay.toLocalDate.isEqual(r.withTimeAtStartOfDay.toLocalDate)).headOption.map { b =>
            toJson(r, b.duration.getMillis)
          }.getOrElse(toJson(r, 0))
        })
      }
      Ok(Json.toJson(combinedMap.map(entry => Json.obj("key" -> entry._1, "values" -> entry._2))))
    }
  }

  def getStatisticsByProject(from: DateTime, to: DateTime, range: Seq[DateTime])(implicit subject: Subject) = {
    bookingByProjectRepository.findByUserIdAndRange(subject.userId, from, to) map { bookings =>
      val combinedMap = bookings.groupBy(_.projectId) map { entry =>
        (entry._1, range.map { r =>
          entry._2.filter(_.day.withTimeAtStartOfDay.toLocalDate.isEqual(r.withTimeAtStartOfDay.toLocalDate)).headOption.map { b =>
            toJson(r, b.duration.getMillis)
          }.getOrElse(toJson(r, 0))
        })
      }
      Ok(Json.toJson(combinedMap.map(entry => Json.obj("key" -> entry._1, "values" -> entry._2))))
    }
  }
}

object TimeBookingStatisticsController extends TimeBookingStatisticsController with Controller with MongoUserBookingStatisticsRepositoryComponent with Security with DefaultSecurityComponent