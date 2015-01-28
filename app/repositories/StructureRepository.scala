package repositories

import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent._
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.json.BSONFormats._
import play.api.libs.json._
import models._

trait StructureRepository extends BaseRepository[Category, CategoryId] {
  def findAllCategories(): Future[Traversable[Category]]
}

class StructureMongoRepository extends BaseReactiveMongoRepository[Category, CategoryId] with StructureRepository {
  def coll = db.collection[JSONCollection]("Category")

  def findAllCategories(): Future[Traversable[Category]] = {
    val sel = Json.obj()
    find(sel) map (_.map(_._1))
  }

}