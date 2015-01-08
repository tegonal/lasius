package models

import reactivemongo.bson.BSONObjectID
import com.tegonal.play.json._
import play.api.libs.json._
import com.tegonal.play.json.TypedId._

case class CategoryId(value: String) extends StringBaseId

object CategoryId {
  implicit val idFormat: Format[CategoryId] = Json.idformat[CategoryId](CategoryId.apply _)
}

case class Category(id: CategoryId) extends BaseEntity[CategoryId]

object Category {
  implicit val categoryFormat: Format[Category] = Json.format[Category]
}