package dao

import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent._
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.json.BSONFormats._
import play.api.libs.json._
import models._

trait ProjectDAO extends BaseDAO[Project, ProjectId] {
}

class ProjectMongoDAO extends BaseReactiveMongoDAO[Project, ProjectId] with ProjectDAO {
  def coll = db.collection[JSONCollection]("Project")
}