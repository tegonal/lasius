package repositories

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.mutable._
import util.MongoSetup
import models._
import org.joda.time.DateTime
import org.joda.time.Duration
import scala.concurrent.Await
import scala.concurrent.duration._
import play.api.libs.json._

@RunWith(classOf[JUnitRunner])
class BookingStatisticsRepositorySpec extends Specification with MongoSetup {
  isolated
  val repository = new BookingByProjectMongoRepository
  val user = UserId("user")
  val day = DateTime.now.withTimeAtStartOfDay
  val projectId = ProjectId("p1")
  "BookingStatistic add" should {
    "insert new record for new unique constraint" in {
      withMongo {
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
    }
    "add to correct previous unique constraint" in {
      withMongo {
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
  }
  "BookingStatistic subtract" should {
    "Remove nothing if no previous entry was found" in {
      withMongo {
        //initialize
        val newDuration = Duration.standardHours(1)
        val newValue = BookingByProject(BookingByProjectId(), user, day, projectId, newDuration)

        //test
        val resultFuture = repository.subtract(newValue)

        //check
        val result = Await.result(resultFuture, DurationInt(15).seconds)
        result === None
      }
    }
    "Remove from correct previous constraint" in {
      withMongo {
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
    }
    "Set to 0 if existing duration is lower than new" in {
      withMongo {
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
  }
}

