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
import models.ProjectId.ProjectReference
import models.UserId.UserReference
import models._
import org.pac4j.core.profile.CommonProfile
import play.api.libs.json.Json.JsValueWrapper
import play.api.libs.json._
import reactivemongo.api.bson.collection.BSONCollection

import javax.inject.Inject
import scala.concurrent._
// Conversions from BSON to JSON extended syntax
//import reactivemongo.play.json.compat.bson2json._

@ImplementedBy(classOf[UserMongoRepository])
trait UserRepository
    extends BaseRepository[User, UserId]
    with DropAllSupport[User, UserId] {
  def assignUserToProject(userId: UserId,
                          organisationReference: OrganisationReference,
                          projectReference: ProjectReference,
                          role: ProjectRole)(implicit
      dbSession: DBSession): Future[Boolean]

  def unassignUserFromProject(userId: UserId,
                              projectReference: ProjectReference)(implicit
      dbSession: DBSession): Future[Boolean]

  def unassignAllUsersFromProject(projectId: ProjectId)(implicit
      dbSession: DBSession): Future[Boolean]

  def assignUserToOrganisation(userId: UserId,
                               organisation: Organisation,
                               role: OrganisationRole,
                               plannedWorkingHours: WorkingHours)(implicit
      dbSession: DBSession): Future[Boolean]

  def unassignUserFromOrganisation(
      userId: UserId,
      organisationReference: OrganisationReference)(implicit
      dbSession: DBSession): Future[Boolean]

  def unassignAllUsersFromOrganisation(organisationId: OrganisationId)(implicit
      dbSession: DBSession): Future[Boolean]

  def findByProject(projectId: ProjectId)(implicit
      dbSession: DBSession): Future[Seq[User]]

  def findByEmail(email: String)(implicit
      dbSession: DBSession): Future[Option[User]]

  def createInitialUserBasedOnProfile(userProfile: CommonProfile,
                                      org: Organisation,
                                      orgRole: OrganisationRole)(implicit
      dbSession: DBSession): Future[User]

  def findByOrganisationAndUserId(id: UserId, orgId: OrganisationId)(implicit
      dbSession: DBSession): Future[Option[User]]

  def findByUserReference(userReference: UserReference)(implicit
      dbSession: DBSession): Future[Option[User]]

  def findByOrganisation(organisationReference: OrganisationReference)(implicit
      dbSession: DBSession): Future[Seq[User]]

  def findAll()(implicit dbSession: DBSession): Future[Seq[User]]

  def updateUserData(userReference: UserReference,
                     personalData: PersonalDataUpdate)(implicit
      dbSession: DBSession): Future[User]

  def updateUserSettings(ence: UserReference, userSettings: UserSettings)(
      implicit dbSession: DBSession): Future[User]

  def updateUserOrganisation(userReference: UserReference,
                             organisationReference: OrganisationReference,
                             updateData: UpdateUserOrganisation)(implicit
      dbSession: DBSession): Future[User]
  def updateOrganisationKey(organisationId: OrganisationId, newKey: String)(
      implicit dbSession: DBSession): Future[Boolean]

  def updateProjectKey(projectId: ProjectId, newKey: String)(implicit
      dbSession: DBSession): Future[Boolean]
}

class UserMongoRepository @Inject() (
    override implicit protected val executionContext: ExecutionContext)
    extends BaseReactiveMongoRepository[User, UserId]
    with UserRepository
    with MongoDropAllSupport[User, UserId]
    with Validation {
  override protected[repositories] def coll(implicit
      dbSession: DBSession): BSONCollection =
    dbSession.db.collection[BSONCollection]("User")

  override def findByEmail(email: String)(implicit
      dbSession: DBSession): Future[Option[User]] = {
    val sel = Json.obj("email" -> email)
    findFirst(sel).map(_.map(_._1))
  }

  private def findByKey(key: String)(implicit
      dbSession: DBSession): Future[Option[User]] = {
    val sel = Json.obj("key" -> key)
    findFirst(sel).map(_.map(_._1))
  }

  override def findByOrganisationAndUserId(id: UserId, orgId: OrganisationId)(
      implicit dbSession: DBSession): Future[Option[User]] = {
    val sel =
      Json.obj("id" -> id, "organisations.organisationReference.id" -> orgId)
    findFirst(sel).map(_.map(_._1))
  }

  private def userSelection(userReference: UserReference) = {
    Json.obj("id" -> userReference.id, "key" -> userReference.key)
  }

  override def findByUserReference(userReference: UserReference)(implicit
      dbSession: DBSession): Future[Option[User]] = {
    val sel = userSelection(userReference)
    find(sel).map(_.headOption.map(_._1))
  }

  override def findAll()(implicit dbSession: DBSession): Future[Seq[User]] = {
    val sel = Json.obj()
    find(sel).map(_.map(_._1).toSeq)
  }

  override def updateUserData(userReference: UserReference,
                              personalData: PersonalDataUpdate)(implicit
      dbSession: DBSession): Future[User] = {
    val sel = userSelection(userReference)

    for {
      _ <- validate(
        personalData.email.isDefined || personalData.lastName.isDefined || personalData.firstName.isDefined,
        s"At least one field needs to be defined as update for user: ${userReference.key}"
      )
      _ <- personalData.email.fold(success())(validateEmail)
      _ <- personalData.email.fold(success())(email =>
        findByEmail(email).flatMap(user =>
          validate(user.fold(true)(u =>
                     u.id == userReference.id && u.key == userReference.key),
                   "Email Address already registered")))
      result <- updateFields(
        sel,
        Seq[Option[(String, JsValueWrapper)]](
          personalData.email.map(value => "email" -> value),
          personalData.firstName.map(value => "firstName" -> value),
          personalData.lastName.map(value => "lastName" -> value)
        ).flatten
      )
      _ <- validate(
        result,
        s"Failed updating personalData of user: ${userReference.key}")
      user <- findByUserReference(userReference).noneToFailed(
        s"Could not find user with id ${userReference.key}")
    } yield user
  }

  override def updateUserSettings(userReference: UserReference,
                                  userSettings: UserSettings)(implicit
      dbSession: DBSession): Future[User] = {
    val sel = userSelection(userReference)

    for {
      result <- updateFields(
        sel,
        Seq[(String, JsValueWrapper)](
          "settings" -> userSettings
        )
      )
      _ <- validate(result,
                    s"Failed updating settings of user: ${userReference.key}")
      user <- findByUserReference(userReference).noneToFailed(
        s"Could not find user with id ${userReference.key}")
    } yield user
  }

  override def updateUserOrganisation(
      userReference: UserReference,
      organisationReference: OrganisationReference,
      updateData: UpdateUserOrganisation)(implicit
      dbSession: DBSession): Future[User] = {
    val sel = userSelection(userReference)

    for {
      user <- findByUserReference(userReference).noneToFailed(
        s"Could not find user with id ${userReference.id.value}")
      _ <- validate(
        user.organisations.exists(
          _.organisationReference == organisationReference),
        s"User ${userReference.id.value} is not assigned to organisation ${organisationReference.id.value}"
      )
      updatedUser = user.copy(organisations = user.organisations.map {
        case t if t.organisationReference == organisationReference =>
          t.copy(plannedWorkingHours = updateData.plannedWorkingHours)
        case t => t
      })
      result <- update(sel,
                       Json.toJson(updatedUser).as[JsObject],
                       upsert = false)
      _ <- validate(
        result,
        s"Failed updating user organisation of user: ${userReference.id.value} and organisation: ${organisationReference.id.value}")
    } yield updatedUser
  }

  override def findByProject(projectId: ProjectId)(implicit
      dbSession: DBSession): Future[Seq[User]] = {
    find(Json.obj("organisations.projects.projectReference.id" -> projectId))
      .map(_.map(_._1).toSeq)
  }

  override def assignUserToProject(userId: UserId,
                                   organisationReference: OrganisationReference,
                                   projectReference: ProjectReference,
                                   role: ProjectRole)(implicit
      dbSession: DBSession): Future[Boolean] = {
    for {
      existingAssignment <- findFirst(
        Json.obj(
          "id"                                     -> userId,
          "organisations.organisationReference.id" -> organisationReference.id,
          "organisations.projects.projectReference.id" -> projectReference.id))
      _ <- validate(
        existingAssignment.isEmpty,
        s"User already assigned to project ${projectReference.key} and organisation ${organisationReference.key}")
      result <- update(
        Json.obj(
          "id"                                     -> userId,
          "organisations.organisationReference.id" -> organisationReference.id),
        Json.obj(
          MongoDBCommandSet.Push -> Json.obj(
            "organisations.$.projects" -> UserProject(None,
                                                      projectReference,
                                                      role))),
        upsert = false
      )
    } yield result
  }

  override def unassignUserFromProject(userId: UserId,
                                       projectReference: ProjectReference)(
      implicit dbSession: DBSession): Future[Boolean] = {
    for {
      result <- update(
        Json.obj("id" -> userId),
        Json.obj(
          MongoDBCommandSet.Pull -> Json.obj(
            "organisations.$[].projects" -> Json.obj(
              "projectReference.id" -> projectReference.id))),
        upsert = false
      )
    } yield result
  }

  override def unassignAllUsersFromProject(projectId: ProjectId)(implicit
      dbSession: DBSession): Future[Boolean] = {
    val sel = Json.obj()
    update(sel,
           Json.obj(
             MongoDBCommandSet.Pull -> Json.obj("organisations.$[].projects" ->
               Json.obj("projectReference.id" -> projectId))),
           upsert = false,
           multi = true)
  }

  override def assignUserToOrganisation(userId: UserId,
                                        organisation: Organisation,
                                        role: OrganisationRole,
                                        plannedWorkingHours: WorkingHours)(
      implicit dbSession: DBSession): Future[Boolean] = {
    val organisationReference = organisation.getReference()
    for {
      existingAssignment <- findFirst(
        Json.obj(
          "id"                                     -> userId,
          "organisations.organisationReference.id" -> organisationReference.id))
      _ <- validate(
        existingAssignment.isEmpty,
        s"User already assigned to organisation ${organisationReference.key}")
      result <- update(
        Json.obj("id" -> userId),
        Json.obj(
          MongoDBCommandSet.Push -> Json.obj(
            "organisations" -> UserOrganisation(organisationReference,
                                                organisation.`private`,
                                                role,
                                                plannedWorkingHours,
                                                Seq()))),
        upsert = false
      )
    } yield result
  }

  override def unassignUserFromOrganisation(
      userId: UserId,
      organisationReference: OrganisationReference)(implicit
      dbSession: DBSession): Future[Boolean] = {
    for {
      users <- findByOrganisation(organisationReference)
      otherActiveAndAdministrators = users.filter(u =>
        u.id != userId &&
          u.active && u.organisations.exists(o =>
            o.role == OrganisationAdministrator && o.organisationReference.id == organisationReference.id))
      _ <- validate(
        otherActiveAndAdministrators.nonEmpty,
        s"RemovalDenied.UserIsLastUserReference"
      )
      result <- update(
        Json.obj("id" -> userId),
        Json.obj(
          MongoDBCommandSet.Pull -> Json.obj("organisations" -> Json.obj(
            "organisationReference.id" -> organisationReference.id))),
        upsert = false
      )
    } yield result
  }

  override def unassignAllUsersFromOrganisation(organisationId: OrganisationId)(
      implicit dbSession: DBSession): Future[Boolean] = {
    val sel = Json.obj()
    update(sel,
           Json.obj(
             MongoDBCommandSet.Pull -> Json.obj("organisations" -> Json.obj(
               "organisationReference.id" -> organisationId))),
           upsert = false,
           multi = true)
  }

  override def findByOrganisation(organisationReference: OrganisationReference)(
      implicit dbSession: DBSession): Future[Seq[User]] = {
    val sel =
      Json.obj(
        "organisations.organisationReference.id" -> organisationReference.id)
    find(sel).map(_.map(_._1).toSeq)
  }

  def validateCreate(registration: User)(implicit
      dbSession: DBSession): Future[Boolean] = {
    for {
      _            <- validateNonBlankString("key", registration.key)
      existingUser <- findByEmail(registration.email)
      _            <- validate(existingUser.isEmpty, s"user_already_registered")
      existingKey  <- findByKey(registration.key)
      _            <- validate(existingKey.isEmpty, s"user_key_already_exists")
    } yield true
  }

  override def createInitialUserBasedOnProfile(userProfile: CommonProfile,
                                               org: Organisation,
                                               orgRole: OrganisationRole)(
      implicit dbSession: DBSession): Future[User] = {
    for {
      newUser <- Future.successful(
        User(
          id = UserId(),
          key = userProfile.getUsername,
          email = userProfile.getEmail,
          firstName = userProfile.getFirstName,
          lastName = userProfile.getFamilyName,
          active = true,
          role = FreeUser,
          organisations = Seq(
            UserOrganisation(
              organisationReference = org.getReference(),
              `private` = org.`private`,
              role = orgRole,
              plannedWorkingHours = WorkingHours(),
              projects = Seq()
            )
          ),
          settings = None
        ))
      _ <- validateCreate(newUser)
      _ <- upsert(newUser)
    } yield newUser
  }

  override def updateOrganisationKey(organisationId: OrganisationId,
                                     newKey: String)(implicit
      dbSession: DBSession): Future[Boolean] = {
    val sel =
      Json.obj("organisations.organisationReference.id" -> organisationId)
    update(sel,
           Json.obj(
             MongoDBCommandSet.Set -> Json.obj(
               "organisations.$.organisationReference.key" -> newKey)),
           upsert = false)
  }

  override def updateProjectKey(projectId: ProjectId, newKey: String)(implicit
      dbSession: DBSession): Future[Boolean] = {
    val sel =
      Json.obj("organisations.projects.projectReference.id" -> projectId)
    update(
      sel,
      Json.obj(MongoDBCommandSet.Set -> Json.obj(
        "organisations.$[].projects.$[element].projectReference.key" -> newKey)),
      upsert = false,
      multi = true,
      arrayFilters = Seq(Json.obj("element.projectReference.id" -> projectId))
    )
  }
}

object UserMongoRepository {
  case class ProjectTags(id: ProjectId,
                         bookingCategories: Set[Tag],
                         active: Boolean)

  implicit val format: OFormat[ProjectTags] = Json.format[ProjectTags]
}
