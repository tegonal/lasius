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

package controllers

import core.{CacheAware, DBSession, DBSupport}
import helpers.UserHelper
import models.{WorkingHours, _}
import org.mindrot.jbcrypt.BCrypt
import org.specs2.mock.Mockito
import play.api.Logging
import play.api.mvc._
import repositories.{SecurityRepositoryComponent, UserRepository}
import util.MockAwaitable

import scala.concurrent.{ExecutionContext, Future}

trait SecurityControllerMock
    extends Logging
    with Security
    with UserHelper
    with SecurityRepositoryComponent
    with MockAwaitable
    with Mockito {
  self: BaseController with CacheAware with DBSupport with SecurityComponent =>
  val userRepository: UserRepository = mockAwaitable[UserRepository]

  val token: String                          = ""
  val userId: UserId                         = UserId()
  val userKey: String                        = "someUserId"
  val userReference: EntityReference[UserId] = EntityReference(userId, userKey)
  val authorized: Future[Boolean]            = Future.successful(true)
  val organisationId: OrganisationId         = OrganisationId()
  val organisationRole: OrganisationRole     = OrganisationAdministrator
  val isOrganisationPrivate: Boolean         = false
  val organisationActive: Boolean            = true
  val organisation: Organisation = Organisation(
    id = organisationId,
    key = "MyOrg",
    `private` = isOrganisationPrivate,
    active = organisationActive,
    createdBy = userReference,
    deactivatedBy = None
  )

  val projectActive: Boolean = true
  val project: Project =
    Project(
      id = ProjectId(),
      key = "project1",
      organisationReference = organisation.reference,
      bookingCategories = Set(SimpleTag(TagId("tag1"))),
      active = projectActive,
      createdBy = userReference,
      deactivatedBy = None
    )
  val password                 = "password"
  val projectRole: ProjectRole = ProjectAdministrator
  val userProject: UserProject = UserProject(
    sharedByOrganisationReference = None,
    projectReference = project.reference,
    role = projectRole
  )

  val plannedWorkingHours: WorkingHours = WorkingHours()
  val userOrganisation: UserOrganisation = UserOrganisation(
    organisationReference = organisation.reference,
    `private` = organisation.`private`,
    role = organisationRole,
    plannedWorkingHours = plannedWorkingHours,
    projects = Seq(userProject)
  )
  val userActive: Boolean = true
  val user: User = User(
    userId,
    userKey,
    email = "user@user.com",
    password = BCrypt.hashpw(password, BCrypt.gensalt()),
    firstName = "test",
    lastName = "user",
    active = userActive,
    role = Administrator,
    organisations = Seq(userOrganisation),
    settings = None
  )
  val authorizationFailedResult: Result = null

  override def HasToken[A](p: BodyParser[A], withinTransaction: Boolean)(
      f: DBSession => Subject => Request[A] => Future[Result])(implicit
      context: ExecutionContext): Action[A] = {
    Action.async(p) { implicit request =>
      withDBSession() { dbSession =>
        checked(f(dbSession)(Subject(token, userReference))(request))
      }
    }
  }

  override def HasUserRole[A, R <: UserRole](role: R,
                                             p: BodyParser[A],
                                             withinTransaction: Boolean)(
      f: DBSession => Subject => User => Request[A] => Future[Result])(implicit
      context: ExecutionContext): Action[A] = {
    Action.async(p) { implicit request =>
      withDBSession() { dbSession =>
        checked(f(dbSession)(Subject(token, userReference))(user)(request))
      }
    }
  }
}
