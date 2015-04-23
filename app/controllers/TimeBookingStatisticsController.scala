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
import play.api.mvc.Result

trait TimeBookingStatisticsController {
  self: Controller with UserBookingStatisticsRepositoryComponent with Security =>

  def getAggregatedStatistics(source: String, from: DateTime, to: DateTime) = HasRole(FreeUser, parse.empty) {
    implicit subject =>
      implicit request => {
        Logger.debug(s"getAggregatedStatistics, source:$source, userId:${subject.userId}, from:$from, to:$to")
        source match {
          case "tag" => getAggregatedStatisticsBySource[BookingByTag, BookingByTagId, TagId](bookingByTagRepository, b => b.tagId, from, to)
          case "category" => getAggregatedStatisticsBySource[BookingByCategory, BookingByCategoryId, CategoryId](bookingByCategoryRepository, b => b.categoryId, from, to)
          case "project" => getAggregatedStatisticsBySource[BookingByProject, BookingByProjectId, ProjectId](bookingByProjectRepository, b => b.projectId, from, to)
          case _ => Future.successful(BadRequest)
        }
      }
  }

  def getAggregatedStatisticsBySource[M <: models.OperatorEntity[I, M], I <: com.tegonal.play.json.TypedId.BaseId[_], V](repository: BookingStatisticRepository[M, I], f: M => V,
    from: DateTime, to: DateTime)(implicit subject: Subject, format: Format[M], formatV: Format[V]) = {
    repository.findByUserIdAndRange(subject.userId, from, to) map { bookings =>
      val aggregatedMap = bookings.groupBy(f) map { entry =>
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
          case "tag" => getStatisticsBySource[BookingByTag, BookingByTagId, TagId](bookingByTagRepository, b => b.tagId, from, to, range)
          case "category" => getStatisticsBySource[BookingByCategory, BookingByCategoryId, CategoryId](bookingByCategoryRepository, b => b.categoryId, from, to, range)
          case "project" => getStatisticsBySource[BookingByProject, BookingByProjectId, ProjectId](bookingByProjectRepository, b => b.projectId, from, to, range)
          case _ => Future.successful(BadRequest)
        }
      }
  }

  private def toJson(day: DateTime, millis: Long) = {
    Json.obj("day" -> Json.toJson(day), "duration" -> Json.toJson(millis))
  }

  def getStatisticsBySource[M <: models.OperatorEntity[I, M], I <: com.tegonal.play.json.TypedId.BaseId[_], V](repository: BookingStatisticRepository[M, I], f: M => V, from: DateTime, to: DateTime, range: Seq[DateTime])(implicit subject: Subject, format: Format[M], formatV: Format[V]) = {
    repository.findByUserIdAndRange(subject.userId, from, to) map { bookings =>
      val combinedMap = bookings.groupBy(f) map { entry =>
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