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

import models.BaseFormat._
import models._
import mongo.EmbedMongo
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, Duration, LocalDate, LocalDateTime}
import org.specs2.mutable._
import util.Awaitable
import play.api.libs.json._
import reactivemongo.api.bson.BSONObjectID

import scala.concurrent.Await
import scala.concurrent.duration._

class BookingStatisticsRepositorySpec
    extends Specification
    with EmbedMongo
    with Awaitable {
  val repository = new BookingByProjectMongoRepository()

  val baseDay = DateTime.now.withTimeAtStartOfDay.toLocalDateTime

  def findByUserDayProject(user: EntityReference[UserId],
                           organisation: EntityReference[OrganisationId],
                           day: LocalDate,
                           project: EntityReference[ProjectId]) = {
    withDBSession() { implicit dbSession =>
      repository
        .find(Json.obj("userReference"            -> user,
                       "organisationReference.id" -> organisation.id,
                       "day"                      -> day,
                       "projectReference.id"      -> project.id),
              limit = 1,
              skip = 0)
        .map(_.headOption.map(_._1))
    }
      .awaitResult()
  }

  "BookingStatistic add" should {
    "insert new record for new unique constraint" in {
      val user =
        EntityReference(UserId(), "userBookingStatisticsRepositorySpec")
      val organisation = EntityReference(OrganisationId(), "organisation1")
      val project      = EntityReference(ProjectId(), "p1")
      val day          = baseDay.plusDays(23).toLocalDate

      // initialize
      val newDuration = Duration.standardHours(1)
      val newValue = BookingByProject(BookingByProjectId(),
                                      user,
                                      organisation,
                                      day,
                                      project,
                                      newDuration)
      // test
      val result =
        withDBSession()(implicit dbSession => repository.add(newValue))
          .awaitResult()
      result === true

      val result2 = findByUserDayProject(user, organisation, day, project)
      result2 must beSome
      result2.get.duration === newDuration
    }

    "add to correct previous unique constraint" in {
      val user =
        EntityReference(UserId(), "userBookingStatisticsRepositorySpec")
      val organisation     = EntityReference(OrganisationId(), "organisation1")
      val existingDuration = Duration.standardHours(2)
      val project          = EntityReference(ProjectId(), "p2")
      val day              = baseDay.plusDays(71).toLocalDate

      // initialize various statistics
      withDBSession() { implicit dbSession =>
        for {
          id <- repository.add(
            BookingByProject(BookingByProjectId(),
                             user,
                             organisation,
                             day,
                             project,
                             existingDuration))
          _ <- repository.add(
            BookingByProject(BookingByProjectId(),
                             user,
                             organisation,
                             day,
                             EntityReference(ProjectId(), "p3"),
                             Duration.standardHours(3)))
          _ <- repository.add(
            BookingByProject(
              BookingByProjectId(),
              EntityReference(UserId(), "userBookingStatisticsRepositorySpec2"),
              organisation,
              day,
              project,
              Duration.standardHours(4)
            ))
          _ <- repository.add(
            BookingByProject(BookingByProjectId(),
                             user,
                             organisation,
                             day.plusDays(1),
                             project,
                             Duration.standardHours(5)))
        } yield {
          id
        }
      }.awaitResult()

      val newDuration = Duration.standardHours(1)
      val newValue = BookingByProject(BookingByProjectId(),
                                      user,
                                      organisation,
                                      day,
                                      project,
                                      newDuration)

      // test
      val result =
        withDBSession()(implicit dbSession => repository.add(newValue))
          .awaitResult()
      result === true

      val result2 = findByUserDayProject(user, organisation, day, project)

      result2 must beSome
      result2.get.duration === existingDuration.plus(newDuration)
    }
  }

  "BookingStatistic subtract" should {
    "Remove negatvie value if no previous entry was found" in {
      val user =
        EntityReference(UserId(), "userBookingStatisticsRepositorySpec")
      val organisation = EntityReference(OrganisationId(), "organisation1")
      // initialize
      val newDuration = Duration.standardHours(1)
      val project     = EntityReference(ProjectId(), "p3")
      val day         = baseDay.plusDays(131).toLocalDate

      val newValue = BookingByProject(BookingByProjectId(),
                                      user,
                                      organisation,
                                      day,
                                      project,
                                      newDuration)

      // test
      val result = withDBSession()(implicit dbSession =>
        repository.subtract(newValue)).awaitResult()
      result === true

      val result2 = findByUserDayProject(user, organisation, day, project)
      result2 must beSome
      result2.get.duration === Duration.standardHours(-1)
    }

    "Remove from correct previous constraint" in {
      val user =
        EntityReference(UserId(), "userBookingStatisticsRepositorySpec")
      val organisation     = EntityReference(OrganisationId(), "organisation1")
      val existingDuration = Duration.standardHours(2)
      val project          = EntityReference(ProjectId(), "p4")
      val day              = baseDay.plusDays(271).toLocalDate

      // initialize various statistics
      withDBSession() { implicit dbSession =>
        for {
          id <- repository.add(
            BookingByProject(BookingByProjectId(),
                             user,
                             organisation,
                             day,
                             project,
                             existingDuration))
          _ <- repository.add(
            BookingByProject(BookingByProjectId(),
                             user,
                             organisation,
                             day,
                             EntityReference(ProjectId(), "p2"),
                             Duration.standardHours(3)))
          _ <- repository.add(
            BookingByProject(
              BookingByProjectId(),
              EntityReference(UserId(), "userBookingStatisticsRepositorySpec2"),
              organisation,
              day,
              project,
              Duration.standardHours(4)
            ))
          _ <- repository.add(
            BookingByProject(BookingByProjectId(),
                             user,
                             organisation,
                             day.plusDays(1),
                             project,
                             Duration.standardHours(5)))
        } yield {
          id
        }
      }.awaitResult()

      val newDuration = Duration.standardHours(1)
      val newValue = BookingByProject(BookingByProjectId(),
                                      user,
                                      organisation,
                                      day,
                                      project,
                                      newDuration)

      // test
      val result = withDBSession()(implicit dbSession =>
        repository.subtract(newValue)).awaitResult()
      result === true

      val result2 = findByUserDayProject(user, organisation, day, project)
      result2 must beSome
      result2.get.duration === existingDuration.minus(newDuration)
    }
  }

  val dateTimeFormat = DateTimeFormat.forPattern("dd.MM.yyyy")

  def date(date: String): LocalDate = {
    LocalDate.parse(date, dateTimeFormat)
  }

  def testFindByUserAndRange[T](from: LocalDate,
                                to: LocalDate,
                                day: LocalDate,
                                aggregationProperty: String,
                                granularity: Granularity)(
      test: List[BookingStats] => T)(implicit
      evidence$1: org.specs2.execute.AsResult[T]): T = {
    val user = EntityReference(UserId(), "userBookingStatisticsRepositorySpec1")
    val organisation = EntityReference(OrganisationId(), "organisation1")

    // initialize
    val b = BookingByProject(BookingByProjectId(),
                             user,
                             organisation,
                             day,
                             EntityReference(ProjectId(), "p1"),
                             Duration.standardHours(1))

    withDBSession()(implicit dbSession => repository.add(b)).awaitResult()

    val find =
      withDBSession()(implicit dbSession =>
        repository.findAggregatedByUserAndRange(user,
                                                organisation.id,
                                                from,
                                                to,
                                                aggregationProperty,
                                                granularity))
        .awaitResult()

    test(find)
  }

  "findAggregatedByUserAndRange" should {

    "find BookingHistory Within range" in {

      // initialize
      val from = date("01.01.2000")
      val to   = date("01.01.2001")
      val day  = date("01.02.2000")

      testFindByUserAndRange(from, to, day, "projectReference.key", Day) {
        result =>
          result must have size (1)
          result.head.category.day must equalTo(Some(day.getDayOfMonth))
          result.head.category.month must equalTo(Some(day.getMonthOfYear))
          result.head.category.year must equalTo(Some(day.getYear))
          result.head.category.week must beNone
      }
    }

    "find BookingHistory Within range, aggregate per week" in {

      // initialize
      val from = date("01.01.2000")
      val to   = date("01.01.2001")
      val day  = date("01.02.2000")

      testFindByUserAndRange(from, to, day, "projectReference.key", Week) {
        result =>
          result must have size (1)
          result.head.category.week must equalTo(Some(day.getWeekOfWeekyear))
          result.head.category.year must equalTo(Some(day.getYear))
          result.head.category.month must beNone
          result.head.category.day must beNone
      }
    }

    "find BookingHistory Within range, aggregate per month" in {

      // initialize
      val from = date("01.01.2000")
      val to   = date("01.01.2001")
      val day  = date("01.02.2000")

      testFindByUserAndRange(from, to, day, "projectReference.key", Month) {
        result =>
          result must have size (1)
          result.head.category.month must equalTo(Some(day.getMonthOfYear))
          result.head.category.year must equalTo(Some(day.getYear))
          result.head.category.week must beNone
          result.head.category.day must beNone
      }
    }

    "find BookingHistory Within range, aggregate per year" in {

      // initialize
      val from = date("01.01.2000")
      val to   = date("01.01.2001")
      val day  = date("01.02.2000")

      testFindByUserAndRange(from, to, day, "projectReference.key", Year) {
        result =>
          result must have size (1)
          result.head.category.year must equalTo(Some(day.getYear))
          result.head.category.week must beNone
          result.head.category.month must beNone
          result.head.category.day must beNone
      }
    }

    "Not find Booking outside of range" in {
      // initialize
      val from = date("01.01.2000")
      val to   = date("01.01.2001")
      val day  = date("01.02.2002")

      testFindByUserAndRange(from, to, day, "projectReference.key", Day) {
        result =>
          result must beEmpty
      }
    }
  }
}
