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

import models._
import mongo.EmbedMongo
import mongo.EmbedMongo.WithMongo
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import play.api.libs.json._

import scala.concurrent.Await
import scala.concurrent.duration._

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