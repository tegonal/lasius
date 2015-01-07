package models

import play.api.libs.json.Json
import com.tegonal.play.json.TypedId._
import play.api.libs.json._

case class UserId(value: String) extends StringBaseId

object UserId {
  implicit val idFormat: Format[UserId] = Json.idformat[UserId](UserId.apply _)
}

case class User(
  id: UserId,
  firstName: String,
  lastName: String,
  active: Boolean)

object User {
  implicit val userFormat = Json.format[User]
}