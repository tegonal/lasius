/*
 *
 * Lasius - Open source time tracker for teams
 * Copyright (c) Tegonal Genossenschaft (https://tegonal.com)
 *
 * This file is part of Lasius.
 *
 * Lasius is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * Lasius is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Lasius. If not, see <https://www.gnu.org/licenses/>.
 */

package repositories

import com.tegonal.play.json.TypedId.BaseId
import core.DBSession
import models.UserId.UserReference
import models._
import play.api.libs.json.{Format, _}

import scala.concurrent.Future

trait PersistentUserViewRepository[T <: BaseEntity[ID], ID <: BaseId[_]] {
  def deleteByUserReference(userReference: UserReference)(implicit
      format: Format[T],
      dbSession: DBSession): Future[Boolean]
}

trait MongoPeristentUserViewRepository[T <: BaseEntity[ID], ID <: BaseId[_]]
    extends PersistentUserViewRepository[T, ID] {
  self: BaseReactiveMongoRepository[T, ID] with BaseRepository[T, ID] =>

  def deleteByUserReference(userReference: UserReference)(implicit
      format: Format[T],
      dbSession: DBSession): Future[Boolean] = {
    val sel = Json.obj("userReference.id" -> userReference.id)
    remove(sel)
  }
}
