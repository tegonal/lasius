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

@ImplementedBy(classOf[ProjectMongoRepository])
trait ProjectRepository
    extends BaseRepositoryWithOrgRef[Project, ProjectId]
    with DropAllSupport[Project, ProjectId]
    with Validation {
  def findByOrganisation(organisationReference: OrganisationReference)(implicit
      dbSession: DBSession): Future[Seq[Project]]

  def findByOrganisationAndProject(organisationReference: OrganisationReference,
                                   projectId: ProjectId)(implicit
      dbSession: DBSession): Future[Option[Project]]

  def create(organisationReference: OrganisationReference,
             createProject: CreateProject)(implicit
      subject: Subject,
      dbSession: DBSession): Future[Project]

  def deactivate(organisationReference: OrganisationReference,
                 projectId: ProjectId)(implicit
      subject: Subject,
      dbSession: DBSession): Future[Boolean]

  def updateOrganisationKey(organisationId: OrganisationId, newKey: String)(
      implicit dbSession: DBSession): Future[Boolean]

  def update(organisationReference: OrganisationReference,
             projectId: ProjectId,
             update: UpdateProject)(implicit
      subject: Subject,
      dbSession: DBSession): Future[Project]
}

class ProjectMongoRepository @Inject() (
    override implicit protected val executionContext: ExecutionContext)
    extends BaseReactiveMongoRepositoryWithOrgRef[Project, ProjectId]
    with ProjectRepository
    with MongoDropAllSupport[Project, ProjectId] {
  override protected[repositories] def coll(implicit
      dbSession: DBSession): BSONCollection =
    dbSession.db.collection[BSONCollection]("Project")

  override def findByOrganisation(organisationReference: OrganisationReference)(
      implicit dbSession: DBSession): Future[Seq[Project]] = {
    find(Json.obj("organisationReference.id" -> organisationReference.id)).map {
      proj =>
        proj.map(_._1).toSeq
    }
  }

  override def findByOrganisationAndProject(
      organisationReference: OrganisationReference,
      projectId: ProjectId)(implicit
      dbSession: DBSession): Future[Option[Project]] = {
    find(
      Json.obj("organisationReference.id" -> organisationReference.id,
               "id"                       -> projectId)).map { proj =>
      proj.map(_._1).headOption
    }
  }

  private def findByOrganisationAndKey(
      organisationReference: OrganisationReference,
      key: String)(implicit dbSession: DBSession): Future[Option[Project]] = {
    find(
      Json.obj("organisationReference.id" -> organisationReference.id,
               "key"                      -> key)).map { proj =>
      proj.map(_._1).headOption
    }
  }

  override def create(organisationReference: OrganisationReference,
                      createProject: CreateProject)(implicit
      subject: Subject,
      dbSession: DBSession): Future[Project] = {
    for {
      existingProject <- findByOrganisationAndKey(organisationReference,
                                                  createProject.key)
      _ <- validate(
        existingProject.isEmpty,
        s"Cannot create project with same key ${createProject.key} in organisation ${organisationReference.id.value}")
      project = Project(
        id = ProjectId(),
        key = createProject.key,
        organisationReference = organisationReference,
        bookingCategories = createProject.bookingCategories,
        active = true,
        createdBy = subject.userReference,
        deactivatedBy = None
      )
      _ <- upsert(project)
    } yield project
  }

  override def deactivate(organisationReference: OrganisationReference,
                          projectId: ProjectId)(implicit
      subject: Subject,
      dbSession: DBSession): Future[Boolean] = {
    updateFields(
      Json.obj("id"                       -> projectId,
               "organisationReference.id" -> organisationReference.id),
      Seq("active" -> false, "deactivatedBy" -> Some(subject.userReference)))
  }

  override def updateOrganisationKey(organisationId: OrganisationId,
                                     newKey: String)(implicit
      dbSession: DBSession): Future[Boolean] = {
    val sel =
      Json.obj("organisationReference.id" -> organisationId)
    update(sel,
           Json.obj(
             MongoDBCommandSet.Set -> Json.obj(
               "organisationReference.key" -> newKey)),
           upsert = false)
  }

  override def update(organisationReference: OrganisationReference,
                      projectId: ProjectId,
                      update: UpdateProject)(implicit
      subject: Subject,
      dbSession: DBSession): Future[Project] = {
    val updateObject: Seq[(String, JsValueWrapper)] = Seq(
      update.key.map(key => "key" -> Json.toJsFieldJsValueWrapper(key)),
      update.bookingCategories.map(tags =>
        "bookingCategories" -> Json.toJsFieldJsValueWrapper(tags))
    ).flatten
    for {
      _ <- update.key.fold(success()) { key =>
        for {
          _ <- validateNonBlankString("key", key)
          existingProject <- findByOrganisationAndKey(organisationReference,
                                                      key)
          result <- validate(
            existingProject.isEmpty,
            s"Cannot update project with duplicate key ${key} in organisation ${organisationReference.id.value}")
        } yield result
      }

      _ <- validate(
        updateObject.nonEmpty,
        s"cannot update project ${projectId.value} in organisation ${organisationReference.id.value}, at least one field must be specified"
      )
      _ <- updateFields(
        Json.obj("id"                       -> projectId,
                 "organisationReference.id" -> organisationReference.id),
        updateObject)
      project <- findByOrganisationAndId(organisationReference, projectId)
        .noneToFailed(
          s"Failed loading updated project ${projectId.value} in organisation ${organisationReference.key}")
    } yield project
  }
}
