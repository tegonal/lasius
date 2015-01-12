package models

import reactivemongo.bson.BSONObjectID

import com.tegonal.play.json._
import play.api.libs.json._
import com.tegonal.play.json.TypedId._

case class TagId(value: String) extends StringBaseId
case class ProjectId(value: String) extends StringBaseId
case class CategoryId(value: String) extends StringBaseId

object CategoryId {
  implicit val idFormat: Format[CategoryId] = Json.idformat[CategoryId](CategoryId.apply _)
}
object ProjectId {
  implicit val idFormat: Format[ProjectId] = Json.idformat[ProjectId](ProjectId.apply _)
}
object TagId {
  implicit val idFormat: Format[TagId] = Json.idformat[TagId](TagId.apply _)
}

case class Tag(id: TagId) extends BaseEntity[TagId]
case class Project(id: ProjectId, tags: Seq[Tag]) extends BaseEntity[ProjectId]
case class Category(id: CategoryId, projects: Seq[Project]) extends BaseEntity[CategoryId]

object Tag {
  implicit val tagFormat: Format[Tag] = Json.format[Tag]
}

object Project {
  implicit val projectFormat: Format[Project] = Json.format[Project]
}

object Category {
  implicit val categoryFormat: Format[Category] = Json.format[Category]
}