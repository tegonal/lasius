package models

import reactivemongo.bson.BSONObjectID
import models.BaseFormat._
import play.api.libs.json._

case class TeamId(value: BSONObjectID = BSONObjectID.generate) extends BaseBSONObjectId {
}

object TeamId {
  implicit val idFormat: Format[TeamId] = BaseFormat.idformat[TeamId](TeamId.apply _)
}

case class Team(id: TeamId, name: String) extends BaseEntity[TeamId]

object Team {
  implicit val teamFormat: Format[Team] = Json.format[Team]
}