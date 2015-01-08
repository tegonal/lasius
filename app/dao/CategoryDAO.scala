package dao

import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent._
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.json.BSONFormats._
import play.api.libs.json._
import models._

trait CategoryDAO extends BaseDAO[Category, CategoryId] {
}

class CategoryMongoDAO extends BaseReactiveMongoDAO[Category, CategoryId] with CategoryDAO {
  def coll = db.collection[JSONCollection]("Category")
}