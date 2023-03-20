/*
 *
 * Lasius - Open source time tracker for teams
 * Copyright (c) Tegonal Genossenschaft (https://tegonal.com)
 *
 * This file is part of Lasius.
 *
 * Lasius is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * Lasius is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Lasius. If not, see <https://www.gnu.org/licenses/>.
 */

package repositories

import models.LocalDateTimeWithTimeZone.DateTimeHelper
import models.{EntityReference, _}
import mongo.EmbedMongo
import org.joda.time.{DateTime, LocalDateTime}
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import reactivemongo.api.bson.BSONObjectID

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class BookingHistoryRepositorySpec extends EmbedMongo {
  val repository = new BookingHistoryMongoRepository()

  val dateTimeFormat: DateTimeFormatter =
    DateTimeFormat.forPattern("dd.MM.yyyy")

  def date(date: String): DateTime = {
    DateTime.parse(date, dateTimeFormat)
  }

  def testFindByUserAndRange[T](from: LocalDateTime,
                                to: LocalDateTime,
                                bookingDates: Seq[(DateTime, DateTime)],
                                limit: Option[Int] = None,
                                skip: Option[Int] = None)(
      test: Iterable[BookingV2] => T)(implicit
      evidence$1: org.specs2.execute.AsResult[T]): T = {
    val user = EntityReference(UserId(), "userBookingHistoryRepositorySpec")
    val org  = EntityReference(OrganisationId(), "team1")
    // initialize
    val bookings = {
      bookingDates.map { case (start, end) =>
        BookingV2(
          BookingId(),
          start.toLocalDateTimeWithZone(),
          Some(end.toLocalDateTimeWithZone()),
          user,
          org,
          EntityReference(ProjectId(), "p1"),
          Set()
        )
      }
    }

    val find = withDBSession() { implicit dbSession =>
      repository.bulkInsert(bookings.toList).flatMap { _ =>
        repository
          .findByUserAndRange(org.id, user, from, to, limit, skip)
      }
    }.awaitResult()
    test(find)
  }

  "findByUserIdAndRange" should {

    "find BookingHistory Within range" in {

      // initialize
      val from  = date("01.01.2000").toLocalDateTime
      val to    = date("01.01.2001").toLocalDateTime
      val start = date("01.02.2000")
      val end   = date("01.03.2000")

      testFindByUserAndRange(from, to, Seq((start, end))) { result =>
        result must have size 1
        result.head.start must equalTo(start.toLocalDateTimeWithZone())
        result.head.end must beSome(end.toLocalDateTimeWithZone())
      }
    }
  }

  "find BookingHistory starting in range" in {
    // initialize
    val from  = date("01.01.2000").toLocalDateTime
    val to    = date("01.01.2001").toLocalDateTime
    val start = date("01.02.2000")
    val end   = date("01.03.2002")

    testFindByUserAndRange(from, to, Seq((start, end))) { result =>
      result must have size 1
      result.head.start must equalTo(start.toLocalDateTimeWithZone())
      result.head.end must beSome(end.toLocalDateTimeWithZone())
    }
  }

  "find BookingHistory ending in range" in {
    // initialize
    val from  = date("01.01.2010").toLocalDateTime
    val to    = date("01.01.2011").toLocalDateTime
    val start = date("01.01.2009")
    val end   = date("01.03.2010")

    testFindByUserAndRange(from, to, Seq((start, end))) { result =>
      result.head.start must equalTo(start.toLocalDateTimeWithZone())
      result.head.end must beSome(end.toLocalDateTimeWithZone())
    }
  }
  "Not find BookingV2 outside of range" in {
    // initialize
    val from  = date("01.01.2000").toLocalDateTime
    val to    = date("01.01.2001").toLocalDateTime
    val start = date("01.02.2002")
    val end   = date("01.03.2003")

    testFindByUserAndRange(from, to, Seq((start, end))) { result =>
      result.find(b =>
        b.start.dateTime
          .isAfter(from) && b.end.get.dateTime
          .isBefore(to)) must beNone
    }
  }

  "cursoring" in {

    "find all result" in {
      val from  = date("01.01.2000").toLocalDateTime
      val to    = date("31.12.2000").toLocalDateTime
      val start = date("01.01.2000")
      val end   = date("02.01.2000")

      val bookingDates = (0 to 2).map { month =>
        (start.plusMonths(month), end.plusMonths(month))
      }

      testFindByUserAndRange(from, to, bookingDates) { result =>
        result must have size 3
      }
    }

    "limit result" in {
      val from  = date("01.01.2000").toLocalDateTime
      val to    = date("31.12.2000").toLocalDateTime
      val start = date("01.01.2000")
      val end   = date("02.01.2000")

      val bookingDates = (0 to 2).map { month =>
        (start.plusMonths(month), end.plusMonths(month))
      }

      testFindByUserAndRange(from, to, bookingDates, limit = Some(1)) {
        result =>
          result must have size 1
          result.head.start.dateTime === start.toLocalDateTime
      }
    }

    "skip records" in {
      val from  = date("01.01.2000").toLocalDateTime
      val to    = date("31.12.2000").toLocalDateTime
      val start = date("01.01.2000")
      val end   = date("02.01.2000")

      val bookingDates = (0 to 2).map { month =>
        (start.plusMonths(month), end.plusMonths(month))
      }

      testFindByUserAndRange(from, to, bookingDates, skip = Some(1)) { result =>
        result must have size 2
        result.head.start.dateTime === start.plusMonths(1).toLocalDateTime
      }
    }
  }
}
