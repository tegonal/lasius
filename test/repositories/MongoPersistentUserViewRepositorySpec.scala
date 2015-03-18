package repositories

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import com.github.athieriot._
import org.specs2.mutable.Specification
import org.joda.time.DateTime
import models._
import util.MongoSetup
import play.api.libs.json._
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.Future
import org.joda.time.format.DateTimeFormat

@RunWith(classOf[JUnitRunner])
class MongoPeristentUserViewRepositorySpec extends Specification with MongoSetup {
  isolated
  "Booking history delete" should {
    "delete all history entries per user" in {
      withMongo {
        val repository = new BookingHistoryMongoRepository
        val user1 = UserId("user1")
        val user2 = UserId("user2")

        //initialize
        val f = for {
          id1 <- repository.insert(Booking(BookingId("b1"), DateTime.now(), None, user1, CategoryId("c1"), ProjectId("p1"), Seq()))
          id2 <- repository.insert(Booking(BookingId("b2"), DateTime.now(), None, user1, CategoryId("c2"), ProjectId("p2"), Seq()))
          id3 <- repository.insert(Booking(BookingId("b3"), DateTime.now(), None, user2, CategoryId("c3"), ProjectId("p3"), Seq()))
        } yield {
          Seq(id1, id2, id3)
        }
        Await.result(f, DurationInt(15).seconds)

        val find1 = repository.find(Json.obj())
        val findAll = Await.result(find1, DurationInt(15).seconds)
        findAll must have size (3)

        val del = repository.deleteByUser(user1)
        val afterDelete = Await.result(del, DurationInt(15).seconds)
        afterDelete must equalTo(true)

        val find2 = repository.find(Json.obj())
        val findAll2 = Await.result(find2, DurationInt(15).seconds)
        findAll2 must have size (1)
      }
    }
  }

  val dateTimeFormat = DateTimeFormat.forPattern("dd.MM.yyyy");

  def date(date: String): DateTime = {
    DateTime.parse(date, dateTimeFormat)
  }

  def testFindByUserIdAndRange[T](from: DateTime, to: DateTime, start: DateTime, end: DateTime)(test: Traversable[Booking] => T)(implicit evidence$1: org.specs2.execute.AsResult[T]) = {
    val repository = new BookingHistoryMongoRepository
    val user = UserId("user1")

    //initialize
    val b = Booking(BookingId("b1"), start, Some(end), user, CategoryId("c1"), ProjectId("p1"), Seq())

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

    "find BookingHistory Within range" in {
      withMongo {

        //initialize
        val from = date("01.01.2000")
        val to = date("01.01.2001")
        val start = date("01.02.2000")
        val end = date("01.03.2000")

        testFindByUserIdAndRange(from, to, start, end) { result =>
          result must have size (1)
          result.head.start must equalTo(start)
          result.head.end must equalTo(Some(end))
        }
      }
    }
  }

  "find BookingHistory starting in range" in {
    withMongo {
      //initialize
      val from = date("01.01.2000")
      val to = date("01.01.2001")
      val start = date("01.02.2000")
      val end = date("01.03.2002")

      testFindByUserIdAndRange(from, to, start, end) { result =>
        result must have size (1)
        result.head.start must equalTo(start)
        result.head.end must equalTo(Some(end))
      }
    }
  }
  "find BookingHistory ending in range" in {
    withMongo {
      //initialize
      val from = date("01.01.2000")
      val to = date("01.01.2001")
      val start = date("01.01.1999")
      val end = date("01.03.2000")

      testFindByUserIdAndRange(from, to, start, end) { result =>
        result must have size (1)
        result.head.start must equalTo(start)
        result.head.end must equalTo(Some(end))
      }
    }
  }
  "Not find Booking outside of range" in {
    withMongo {
      //initialize
      val from = date("01.01.2000")
      val to = date("01.01.2001")
      val start = date("01.02.2002")
      val end = date("01.03.2003")

      testFindByUserIdAndRange(from, to, start, end) { result =>
        result must have size (0)
      }
    }
  }
}