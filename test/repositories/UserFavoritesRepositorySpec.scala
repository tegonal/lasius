package repositories

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.mutable._
import com.github.athieriot.EmbedConnection
import mongo.MongoSetup
import models.UserId
import scala.concurrent.Await
import scala.concurrent.duration._
import models._
import com.github.athieriot.CleanAfterExample
import mongo.WithMongo

@RunWith(classOf[JUnitRunner])
class UserFavoritesRepositorySpec extends Specification {

  val repository = new UserFavoritesMongoRepository
  "UserFavoritesRepository getById" should {
    "return empty favorites if no favorites where found" in new WithMongo {
      //initialize
      val user = UserId("user")

      //execute
      val find = repository.findById(user)
      val result = Await.result(find, DurationInt(15).seconds)

      //test
      result === UserFavorites(user, Seq())
    }
  }
  "UserFavoritesRepository addFavorite" should {
    "insert new userfavorites if no favorites did exist so far" in new WithMongo {
      //initialize
      val user = UserId("user")
      val bookingStub = BookingStub(CategoryId("cat"), ProjectId("p1"), Seq(TagId("tag1")))

      //execute
      val find = repository.addFavorite(user, bookingStub.categoryId, bookingStub.projectId, bookingStub.tags)
      val result = Await.result(find, DurationInt(15).seconds)

      //test
      result === UserFavorites(user, Seq(bookingStub))
    }
    "add new userfavorites to existing favorites" in new WithMongo {
      //initialize
      val user = UserId("user")
      val existingBookingStub = BookingStub(CategoryId("cat2"), ProjectId("p2"), Seq(TagId("tag2")))
      val bookingStub = BookingStub(CategoryId("cat"), ProjectId("p1"), Seq(TagId("tag1")))

      val f = for {
        id <- repository.insert(UserFavorites(user, Seq(existingBookingStub)))
      } yield {
        id
      }
      Await.result(f, DurationInt(15).seconds)

      //execute
      val find = repository.addFavorite(user, bookingStub.categoryId, bookingStub.projectId, bookingStub.tags)
      val result = Await.result(find, DurationInt(15).seconds)

      //test
      result === UserFavorites(user, Seq(existingBookingStub, bookingStub))
    }
  }
  "UserFavoritesRepository removeFavorite" should {
    "throw exception if booking stub could't get removed" in new WithMongo {
      //initialize
      val user = UserId("user")
      val bookingStub = BookingStub(CategoryId("cat"), ProjectId("p1"), Seq(TagId("tag1")))

      //execute
      repository.removeFavorite(user, bookingStub) must throwA[RuntimeException]
    }
    "remove booking stub" in new WithMongo {
      //initialize
      val user = UserId("user")
      val bookingStub1 = BookingStub(CategoryId("cat"), ProjectId("p1"), Seq(TagId("tag1")))
      val bookingStub2 = BookingStub(CategoryId("cat2"), ProjectId("p2"), Seq(TagId("tag2")))

      val f = for {
        id <- repository.insert(UserFavorites(user, Seq(bookingStub1, bookingStub2)))
      } yield {
        id
      }
      Await.result(f, DurationInt(15).seconds)

      //execute
      val find = repository.removeFavorite(user, bookingStub1)
      val result = Await.result(find, DurationInt(15).seconds)

      //test
      result === UserFavorites(user, Seq(bookingStub2))
    }
  }
}