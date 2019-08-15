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

import models.{User, UserId}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json._
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent._

trait UserRepository extends BaseRepository[User, UserId] {

  def findByEmail(email: String): Future[Option[User]]
  
  def findAll(): Future[Seq[User]]
}

class UserMongoRepository extends BaseReactiveMongoRepository[User, UserId] with UserRepository {
  def coll = db.map(_.collection[JSONCollection]("User"))

  def findByEmail(email: String): Future[Option[User]] = {
    val sel = Json.obj("email" -> email)
    findFirst(sel) map (_.map(_._1))
  }
  
  def findAll(): Future[Seq[User]] = {
    val sel = Json.obj()
    find(sel) map (_.map(_._1).toSeq)
  }
}