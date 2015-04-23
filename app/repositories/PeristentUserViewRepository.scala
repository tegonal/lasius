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

import models._
import play.api.libs.json.Format
import scala.concurrent.ExecutionContext
import com.tegonal.play.json.TypedId.BaseId
import scala.concurrent.Future
import org.joda.time.DateTime
import play.api.libs.json._
import reactivemongo.core.commands._
import repositories.MongoDBCommandSet._
import play.api.Logger

trait PersistentUserViewRepository[T <: BaseEntity[ID], ID <: BaseId[_]] {
  def deleteByUser(userId: UserId)(implicit ctx: ExecutionContext, format: Format[T]): Future[Boolean]
}

trait MongoPeristentUserViewRepository[T <: BaseEntity[ID], ID <: BaseId[_]] extends PersistentUserViewRepository[T, ID] {
  self: BaseReactiveMongoRepository[T, ID] with BaseRepository[T, ID] =>

  def deleteByUser(userId: UserId)(implicit ctx: ExecutionContext, format: Format[T]): Future[Boolean] = {
    val sel = Json.obj("userId" -> userId)
    coll.remove(sel) map {
      _ match {
        case LastError(ok, _, _, _, _, _, _) => ok
        case e => false
      }
    }
  }

}