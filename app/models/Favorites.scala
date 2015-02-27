package models

import reactivemongo.bson.BSONObjectID
import models.BaseFormat._
import play.api.libs.json._

case class UserFavorites(id: UserId, favorites: Seq[BookingStub]) extends BaseEntity[UserId]

object UserFavorites {
  implicit val favoritesFormat: Format[UserFavorites] = Json.format[UserFavorites]
}