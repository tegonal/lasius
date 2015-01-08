package models

import reactivemongo.bson.BSONObjectID
import com.tegonal.play.json._
import play.api.libs.json._
import com.tegonal.play.json.TypedId._

case class ProjectId(value: String) extends StringBaseId

object ProjectId {
  implicit val idFormat: Format[ProjectId] = Json.idformat[ProjectId](ProjectId.apply _)
}

case class Project(id: ProjectId) extends BaseEntity[ProjectId]

object Project {
  implicit val projectFormat: Format[Project] = Json.format[Project]
}