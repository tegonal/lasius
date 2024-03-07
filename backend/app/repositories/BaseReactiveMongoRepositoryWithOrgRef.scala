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
import models.BaseEntityWithOrgRelation
import models.OrganisationId.OrganisationReference
import play.api.libs.json.Json
import play.api.libs.json.Json.JsValueWrapper

import scala.concurrent.{ExecutionContext, Future}

trait BaseRepositoryWithOrgRef[T <: BaseEntityWithOrgRelation[ID],
                               ID <: BaseId[_]]
    extends BaseRepository[T, ID] {
  def findByOrganisationAndId(organisationReference: OrganisationReference,
                              id: ID)(implicit
      dbSession: DBSession,
      executionContext: ExecutionContext,
      fact: ID => JsValueWrapper): Future[Option[T]]
}

trait BaseReactiveMongoRepositoryWithOrgRef[T <: BaseEntityWithOrgRelation[ID],
                                            ID <: BaseId[_]]
    extends BaseReactiveMongoRepository[T, ID]
    with BaseRepositoryWithOrgRef[T, ID] {
  override def findByOrganisationAndId(
      organisationReference: OrganisationReference,
      id: ID)(implicit
      dbSession: DBSession,
      executionContext: ExecutionContext,
      fact: ID => JsValueWrapper): Future[Option[T]] = {
    findFirst(
      Json.obj("organisationReference.id" -> organisationReference.id,
               "id"                       -> fact(id))).map { result =>
      result.map(_._1)
    }
  }
}
