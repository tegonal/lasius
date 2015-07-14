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
package repositories

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.mutable._
import models._
import org.joda.time.DateTime
import org.joda.time.Duration
import scala.concurrent.Await
import scala.concurrent.duration._
import play.api.libs.json._
import org.joda.time.format.DateTimeFormat
import mongo.EmbedMongo
import mongo.EmbedMongo.WithMongo

@RunWith(classOf[JUnitRunner])
class BookingStatisticsRepositorySpec extends EmbedMongo {
  val repository = new BookingByProjectMongoRepository
  val user = UserId("user")
  val day = DateTime.now.withTimeAtStartOfDay

  def findByUserDayProject(user: UserId, day: DateTime, projectId: ProjectId) = {
    Await.result(repository.find(Json.obj("userId" -> user, "day" -> day, "projectId" -> projectId), 1, 0).map(x => x.headOption.map(_._1)), DurationInt(15).seconds)
  }

  "BookingStatistic add" should {
    "insert new record for new unique constraint" in new WithMongo {
      val projectId = ProjectId("p1")

      //initialize
      val newDuration = Duration.standardHours(1)
      val newValue = BookingByProject(BookingByProjectId(), user, day, projectId, newDuration)

      //test
      val resultFuture = repository.add(newValue)

      //check
      val result = Await.result(resultFuture, DurationInt(15).seconds)
      result === true

      val result2 = findByUserDayProject(user, day, projectId)
      result2 !== None
      result2.get.duration === newDuration
    }
    "add to correct previous unique constraint" in new WithMongo {
      val existingDuration = Duration.standardHours(2)
      val projectId = ProjectId("p2")

      //initialize various statistics
      val f = for {
        id <- repository.insert(BookingByProject(BookingByProjectId(), user, day, projectId, existingDuration))
        id2 <- repository.insert(BookingByProject(BookingByProjectId(), user, day, ProjectId("p2"), Duration.standardHours(3)))
        id3 <- repository.insert(BookingByProject(BookingByProjectId(), UserId("user2"), day, projectId, Duration.standardHours(4)))
        id4 <- repository.insert(BookingByProject(BookingByProjectId(), user, day.plusDays(1), projectId, Duration.standardHours(5)))
      } yield {
        id
      }
      Await.result(f, DurationInt(15).seconds)

      val newDuration = Duration.standardHours(1)
      val newValue = BookingByProject(BookingByProjectId(), user, day, projectId, newDuration)

      //test
      val resultFuture = repository.add(newValue)

      //check
      val result = Await.result(resultFuture, DurationInt(15).seconds)
      result === true

      val result2 = findByUserDayProject(user, day, projectId)
      result2 !== None
      result2.get.duration === existingDuration.plus(newDuration)
    }
  }
  "BookingStatistic subtract" should {
    "Remove negatvie value if no previous entry was found" in new WithMongo {
      //initialize
      val newDuration = Duration.standardHours(1)
      val projectId = ProjectId("p3")
      val newValue = BookingByProject(BookingByProjectId(), user, day, projectId, newDuration)

      //test
      val resultFuture = repository.subtract(newValue)

      //check
      val result = Await.result(resultFuture, DurationInt(15).seconds)
      result === true

      val result2 = findByUserDayProject(user, day, projectId)
      result2 !== None
      result2.get.duration === Duration.standardHours(-1)
    }
    "Remove from correct previous constraint" in new WithMongo {
      val existingDuration = Duration.standardHours(2)
      val projectId = ProjectId("p4")

      //initialize various statistics
      val f = for {
        id <- repository.insert(BookingByProject(BookingByProjectId(), user, day, projectId, existingDuration))
        id2 <- repository.insert(BookingByProject(BookingByProjectId(), user, day, ProjectId("p2"), Duration.standardHours(3)))
        id3 <- repository.insert(BookingByProject(BookingByProjectId(), UserId("user2"), day, projectId, Duration.standardHours(4)))
        id4 <- repository.insert(BookingByProject(BookingByProjectId(), user, day.plusDays(1), projectId, Duration.standardHours(5)))
      } yield {
        id
      }
      Await.result(f, DurationInt(15).seconds)

      val newDuration = Duration.standardHours(1)
      val newValue = BookingByProject(BookingByProjectId(), user, day, projectId, newDuration)

      //test
      val resultFuture = repository.subtract(newValue)

      //check
      val result = Await.result(resultFuture, DurationInt(15).seconds)
      result === true

      val result2 = findByUserDayProject(user, day, projectId)
      result2 !== None
      result2.get.duration === existingDuration.minus(newDuration)
    }
  }

  val dateTimeFormat = DateTimeFormat.forPattern("dd.MM.yyyy");
  def date(date: String): DateTime = {
    DateTime.parse(date, dateTimeFormat)
  }

  def testFindByUserIdAndRange[T](from: DateTime, to: DateTime, day: DateTime)(test: Traversable[BookingByProject] => T)(implicit evidence$1: org.specs2.execute.AsResult[T]) = {
    val user = UserId("user1")

    //initialize
    val b = BookingByProject(BookingByProjectId(), user, day, ProjectId("p1"), Duration.standardHours(1))

    val f = repository.insert(b)
    Await.result(f, DurationInt(15).seconds)

    val find = repository.findByUserIdAndRange(user, from, to)
    val findSync = Await.result(find, DurationInt(15).seconds)

    test(findSync)
  }

  "findByUserIdAndRange" should {

    "find BookingHistory Within range" in new WithMongo {

      //initialize
      val from = date("01.01.2000")
      val to = date("01.01.2001")
      val day = date("01.02.2000")

      testFindByUserIdAndRange(from, to, day) { result =>
        result must have size (1)
        result.head.day must equalTo(day)
      }
    }
  }

  "Not find Booking outside of range" in new WithMongo {
    //initialize
    val from = date("01.01.2000")
    val to = date("01.01.2001")
    val day = date("01.02.2002")

    testFindByUserIdAndRange(from, to, day) { result =>
      result must have size (0)
    }
  }
}

