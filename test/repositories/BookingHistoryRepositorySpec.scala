package repositories

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import com.github.athieriot._
import org.specs2.mutable.Specification
import org.joda.time.DateTime
import models._
import play.api.libs.json._
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.Future
import org.joda.time.format.DateTimeFormat
import mongo.EmbedMongo
import mongo.EmbedMongo.WithMongo

@RunWith(classOf[JUnitRunner])
class BookingHistoryRepositorySpec extends EmbedMongo {
  val repository = new BookingHistoryMongoRepository

  val dateTimeFormat = DateTimeFormat.forPattern("dd.MM.yyyy");

  def date(date: String): DateTime = {
    DateTime.parse(date, dateTimeFormat)
  }

  def testFindByUserIdAndRange[T](from: DateTime, to: DateTime, start: DateTime, end: DateTime)(test: Traversable[Booking] => T)(implicit evidence$1: org.specs2.execute.AsResult[T]) = {
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

    "find BookingHistory Within range" in new WithMongo {

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

  "find BookingHistory starting in range" in new WithMongo {
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
  "find BookingHistory ending in range" in new WithMongo {
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
  "Not find Booking outside of range" in new WithMongo {
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