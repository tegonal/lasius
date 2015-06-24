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
import com.github.athieriot.EmbedConnection
import models.UserId
import scala.concurrent.Await
import scala.concurrent.duration._
import models._
import com.github.athieriot.CleanAfterExample
import play.api.test.FakeApplication
import mongo.EmbedMongo
import mongo.EmbedMongo.WithMongo

@RunWith(classOf[JUnitRunner])
class UserFavoritesRepositorySpec extends EmbedMongo {

  val repository = new UserFavoritesMongoRepository
  "UserFavoritesRepository getById" should {
    "return empty favorites if no favorites where found" in new WithMongo {
      //initialize
      val user = UserId("user")

      //execute
      val find = repository.getByUser(user)
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

      val f = repository.insert(UserFavorites(user, Seq(existingBookingStub)))
      Await.result(f, DurationInt(15).seconds)

      //execute
      val find = repository.addFavorite(user, bookingStub.categoryId, bookingStub.projectId, bookingStub.tags)
      val result = Await.result(find, DurationInt(15).seconds)

      //test
      result === UserFavorites(user, Seq(existingBookingStub, bookingStub))
    }
  }
  "UserFavoritesRepository removeFavorite" should {
    "return empty sub if userid does not exists" in new WithMongo {
      //initialize
      val user = UserId("user")
      val bookingStub = BookingStub(CategoryId("cat"), ProjectId("p1"), Seq(TagId("tag1")))

      //execute
      val f = repository.removeFavorite(user, bookingStub)
      val result = Await.result(f, DurationInt(15).seconds)
      result === UserFavorites(user, Seq())
    }
    "remove booking stub" in new WithMongo {
      //initialize
      val user = UserId("user")
      val bookingStub1 = BookingStub(CategoryId("cat"), ProjectId("p1"), Seq(TagId("tag1")))
      val bookingStub2 = BookingStub(CategoryId("cat2"), ProjectId("p2"), Seq(TagId("tag2")))

      val f = repository.insert(UserFavorites(user, Seq(bookingStub1, bookingStub2)))
      Await.result(f, DurationInt(15).seconds)

      //execute
      val find = repository.removeFavorite(user, bookingStub1)
      val result = Await.result(find, DurationInt(15).seconds)

      //test
      result === UserFavorites(user, Seq(bookingStub2))
    }
  }
}