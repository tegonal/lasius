package models

import com.tegonal.play.json._
import play.api.libs.json._
import com.tegonal.play.json.TypedId._

case class UserId(value: String) extends StringBaseId

object UserId {
  implicit val idFormat: Format[UserId] = Json.idformat[UserId](UserId.apply _)
}

case class User(
  id: UserId,
  email: String,
  password: String,
  firstName: String,
  lastName: String,
  active: Boolean,
  role: Role,
  teams: Seq[Team],
  categories: Seq[Category]) extends BaseEntity[UserId]

object User {
  implicit val userFormat = Json.format[User]
}