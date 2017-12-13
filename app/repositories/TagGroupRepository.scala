/*   __                          __                                          *\
*   / /____ ___ ____  ___  ___ _/ /       lasius                      *
*  / __/ -_) _ `/ _ \/ _ \/ _ `/ /        contributed by tegonal              *
*  \__/\__/\_, /\___/_//_/\_,_/_/         http://tegonal.com/                 *
*         /___/                                                               *
*                                                                             *
* This program is free software: you can redistribute it and/or modify it     *
* under the terms of the GNU General Public License as published by    *
* the Free Software Foundation, either version 3 of the License,              *
* or (at your option) any later version.                                      *
*                                                                             *
* This program is distributed in the hope that it will be useful, but         *
* WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY  *
* or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for *
* more details.                                                               *
*                                                                             *
* You should have received a copy of the GNU General Public License along     *
* with this program. If not, see http://www.gnu.org/licenses/                 *
*                                                                             *
\*                                                                           */
package repositories

import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent._
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.json.BSONFormats._
import play.api.libs.json._
import models._
import play.api.Logger
import org.joda.time.DateTime
import repositories.MongoDBCommandSet._
import reactivemongo.core.commands.LastError
import akka.actor.Actor

trait TagGroupRepository extends BaseRepository[TagGroup, TagGroupId] with PersistentUserViewRepository[TagGroup, TagGroupId] {
  /**
   * Find all tag groups which are part of the provided set of tags (is a subset of)
   */
  def findByTags(tags: Set[TagId]): Future[Traversable[TagGroup]]
}

class TagGroupMongoRepository extends BaseReactiveMongoRepository[TagGroup, TagGroupId] with TagGroupRepository
  with MongoPeristentUserViewRepository[TagGroup, TagGroupId] {
  def coll = db.collection[JSONCollection]("TagGroup")

  def findByTags(tags: Set[TagId]): Future[Traversable[TagGroup]] = {
    /*
     * In MongoDb, for array field:
     * "$in:[...]" means "intersection" or "any element in",
     * "$all:[...]" means "subset" or "contain",
     * "$elemMatch:{...}" means "any element match"
     * "$not:{$elemMatch:{$nin:[...]}}" means "superset" or "in"    
    */
    find(Json.obj("$not" -> Json.obj("$$elemMatch" -> Json.obj("$nin" -> tags)))) map (_.map(_._1))
  }
}