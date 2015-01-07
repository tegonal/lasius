package dao

import models.User
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent._
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.json.BSONFormats._
import play.api.libs.json._
import play.api.Logger
import reactivemongo.core.commands.LastError
import models.UserId
import play.api.libs.json.Json.JsValueWrapper

trait UserDAOComponent {
  val userDAO: UserDAO
}

trait MongoUserDAOComponent extends UserDAOComponent {
  val userDAO = new UserMongoDAO
}

trait UserDAO extends BaseDAO[User, UserId] {
}

class UserMongoDAO extends BaseReactiveMongoDAO[User, UserId] with UserDAO {
  def coll = db.collection[JSONCollection]("User")
}