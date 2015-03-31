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
  val projectId = ProjectId("p1")
  "BookingStatistic add" should {
    "insert new record for new unique constraint" in new WithMongo {
      //initialize
      val newDuration = Duration.standardHours(1)
      val newValue = BookingByProject(BookingByProjectId(), user, day, projectId, newDuration)

      //test
      val resultFuture = repository.add(newValue)

      //check
      val result = Await.result(resultFuture, DurationInt(15).seconds)
      result.day === day
      result.projectId === projectId
      result.userId === user
      result.duration === newDuration
    }
    "add to correct previous unique constraint" in new WithMongo {
      val existingDuration = Duration.standardHours(2)

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
      result.day === day
      result.projectId === projectId
      result.userId === user
      result.duration === existingDuration.plus(newDuration)
    }
  }
  "BookingStatistic subtract" should {
    "Remove nothing if no previous entry was found" in new WithMongo {
      //initialize
      val newDuration = Duration.standardHours(1)
      val newValue = BookingByProject(BookingByProjectId(), user, day, projectId, newDuration)

      //test
      val resultFuture = repository.subtract(newValue)

      //check
      val result = Await.result(resultFuture, DurationInt(15).seconds)
      result === None
    }
    "Remove from correct previous constraint" in new WithMongo {
      val existingDuration = Duration.standardHours(2)

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
      result !== None
      result.get.day === day
      result.get.projectId === projectId
      result.get.userId === user
      result.get.duration === existingDuration.minus(newDuration)
    }
    "Set to 0 if existing duration is lower than new" in new WithMongo {
      val existingDuration = Duration.standardHours(2)

      //initialize various statistics
      val f = for {
        id <- repository.insert(BookingByProject(BookingByProjectId(), user, day, projectId, existingDuration))
      } yield {
        id
      }
      Await.result(f, DurationInt(15).seconds)

      val newDuration = Duration.standardHours(3)
      val newValue = BookingByProject(BookingByProjectId(), user, day, projectId, newDuration)

      //test
      val resultFuture = repository.subtract(newValue)

      //check
      val result = Await.result(resultFuture, DurationInt(15).seconds)
      result !== None
      result.get.day === day
      result.get.projectId === projectId
      result.get.userId === user
      result.get.duration === new Duration(0)
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

    val f = for {
      id <- repository.insert(b)
    } yield {
      id
    }

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

