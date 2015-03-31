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
import play.api.test.FakeApplication
import mongo.EmbedMongo
import mongo.EmbedMongo.WithMongo

@RunWith(classOf[JUnitRunner])
class MongoPeristentUserViewRepositorySpec extends EmbedMongo {

  val repository = new BookingHistoryMongoRepository
  "Booking history delete" should {
    "delete all history entries per user" in new WithMongo {
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