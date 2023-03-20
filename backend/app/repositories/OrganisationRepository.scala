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

import com.google.inject.ImplementedBy
import core.{DBSession, Validation}
import models.OrganisationId.OrganisationReference
import models._
import play.api.libs.json.Json
import play.api.libs.json.Json.JsValueWrapper
import reactivemongo.api.bson.collection.BSONCollection

import javax.inject.Inject
import scala.concurrent._

@ImplementedBy(classOf[OrganisationMongoRepository])
trait OrganisationRepository
    extends BaseRepository[Organisation, OrganisationId]
    with DropAllSupport[Organisation, OrganisationId] {
  def create(key: String, `private`: Boolean)(implicit
      subject: Subject,
      dbSession: DBSession): Future[Organisation]

  def update(organisationReference: OrganisationReference,
             update: UpdateOrganisation)(implicit
      subject: Subject,
      dbSession: DBSession): Future[Organisation]

  def deactivate(organisationReference: OrganisationReference)(implicit
      subject: Subject,
      dbSession: DBSession): Future[Boolean]

  def findByKey(key: String)(implicit
      dbSession: DBSession): Future[Option[Organisation]]
}

class OrganisationMongoRepository @Inject() (
    override implicit protected val executionContext: ExecutionContext)
    extends BaseReactiveMongoRepository[Organisation, OrganisationId]
    with OrganisationRepository
    with MongoDropAllSupport[Organisation, OrganisationId]
    with Validation {
  override protected[repositories] def coll(implicit
      dbSession: DBSession): BSONCollection =
    dbSession.db.collection[BSONCollection]("Organisation")

  override def findByKey(key: String)(implicit
      dbSession: DBSession): Future[Option[Organisation]] = {
    find(Json.obj("key" -> key)).map(_.map(_._1).headOption)
  }

  override def create(key: String, `private`: Boolean)(implicit
      subject: Subject,
      dbSession: DBSession): Future[Organisation] = {
    for {
      _                    <- validateNonBlankString("key", key)
      existingOrganisation <- findByKey(key)
      _ <- validate(existingOrganisation.isEmpty,
                    s"Cannot create organisation with same key $key")
      organisation = Organisation(id = OrganisationId(),
                                  key = key,
                                  `private` = `private`,
                                  active = true,
                                  createdBy = subject.userReference,
                                  deactivatedBy = None)
      _ <- upsert(organisation)
    } yield organisation
  }

  override def deactivate(organisationReference: OrganisationReference)(implicit
      subject: Subject,
      dbSession: DBSession): Future[Boolean] = {
    updateFields(
      Json.obj("id" -> organisationReference.id),
      Seq("active"  -> false, "deactivatedBy" -> Some(subject.userReference)))
  }

  override def update(organisationReference: OrganisationReference,
                      update: UpdateOrganisation)(implicit
      subject: Subject,
      dbSession: DBSession): Future[Organisation] = {
    val updateObject: Seq[(String, JsValueWrapper)] = Seq(
      update.key.map(key => "key" -> Json.toJsFieldJsValueWrapper(key))
    ).flatten
    for {
      _ <- update.key.fold(success()) { key =>
        for {
          _                    <- validateNonBlankString("key", key)
          existingOrganisation <- findByKey(key)
          result <- validate(existingOrganisation.isEmpty,
                             s"Cannot create organisation with same key $key")
        } yield result
      }
      _ <- validate(
        !updateObject.isEmpty,
        s"cannot update organisation ${organisationReference.key}, at least one field must be specified")
      _ <- updateFields(Json.obj("id" -> organisationReference.id),
                        updateObject)
      org <- findById(organisationReference.id).noneToFailed(
        s"Failed loading updated organisation ${organisationReference.key}")
    } yield org
  }
}
