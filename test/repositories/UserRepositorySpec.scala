package repositories

import org.specs2.mutable._
import com.github.athieriot.EmbedConnection
import util.MongoSetup
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import models.User
import models.UserId
import models.Role
import models.FreeUser
import scala.concurrent.Await
import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
class UserRepositorySpec extends Specification with EmbedConnection with MongoSetup {
  isolated
  val repository = new UserMongoRepository
  "UserRepository findByEmail" should {
    "find user by email" in {
      withMongo {
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
    }
    "find none" in {
      withMongo {
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
}