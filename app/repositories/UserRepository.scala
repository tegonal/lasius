package repositories

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

trait UserRepository extends BaseRepository[User, UserId] {
}

class UserMongoRepository extends BaseReactiveMongoRepository[User, UserId] with UserRepository {
  def coll = db.collection[JSONCollection]("User")
}