package repositories

import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent._
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.json.BSONFormats._
import play.api.libs.json._
import models._

trait StructureRepository extends BaseRepository[Category, CategoryId] {
}

class StructureMongoRepository extends BaseReactiveMongoRepository[Category, CategoryId] with StructureRepository {
  def coll = db.collection[JSONCollection]("Category")
}