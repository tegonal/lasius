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

import org.specs2.mutable._
import com.github.athieriot.EmbedConnection
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import models.User
import models.UserId
import models.Role
import models.FreeUser
import scala.concurrent.Await
import scala.concurrent.duration._
import mongo.EmbedMongo
import mongo.EmbedMongo.WithMongo

@RunWith(classOf[JUnitRunner])
class UserRepositorySpec extends EmbedMongo {
  val repository = new UserMongoRepository
  "UserRepository findByEmail" should {
    "find user by email" in new WithMongo {
      val email = "email"
      val user = User(UserId("user"), email, "pwd", "firstname", "lastname", true, FreeUser, Seq(), Seq())

      //initialize
      val f = for {
        id <- repository.insert(user)
      } yield {
        id
      }
      Await.result(f, DurationInt(15).seconds)

      val find = repository.findByEmail(email)
      val result = Await.result(find, DurationInt(15).seconds)
      result === Some(user)
    }
    "find none" in new WithMongo {
      val email = "email"
      val user = User(UserId("user"), email, "pwd", "firstname", "lastname", true, FreeUser, Seq(), Seq())

      //initialize
      val f = for {
        id <- repository.insert(user)
      } yield {
        id
      }
      Await.result(f, DurationInt(15).seconds)

      val find = repository.findByEmail("email2")
      val result = Await.result(find, DurationInt(15).seconds)
      result === None
    }
  }
}