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

import com.google.inject.ImplementedBy
import core.{DBSession, SystemServices}
import helpers.UserHelper
import models.UserId.UserReference
import models._
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.json._
import play.api.mvc._
import repositories.{SecurityRepositoryComponent, UserRepository}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[DefaultAuthConfig])
trait AuthConfig {

  /** Map usertype to permission role.
    */
  def authorizeUser(user: User, role: UserRole)(implicit
      ctx: ExecutionContext): Future[Boolean]

  def authorizeUserOrganisation(userOrganisation: UserOrganisation,
                                role: OrganisationRole)(implicit
      ctx: ExecutionContext): Future[Boolean]

  def authorizeUserProject(userProject: UserProject, role: ProjectRole)(implicit
      ctx: ExecutionContext): Future[Boolean]

  /** Resolve user based on bson object id
    */
  def resolveUser(userReference: UserReference)(implicit
      context: ExecutionContext,
      dbSession: DBSession): Future[Option[User]]

  /** Defined handling of authorizationFailed
    */
  def authorizationFailed(request: RequestHeader)(implicit
      context: ExecutionContext): Future[Result]
}

class DefaultAuthConfig @Inject() (controllerComponents: ControllerComponents,
                                   systemServices: SystemServices,
                                   override val userRepository: UserRepository)
    extends AbstractController(controllerComponents)
    with AuthConfig
    with UserHelper
    with SecurityRepositoryComponent {

  /** Map usertype to permission role.
    */
  override def authorizeUser(user: User, role: UserRole)(implicit
      ctx: ExecutionContext): Future[Boolean] =
    Future.successful((user.role, role) match {
      case (x, y) => x == y || x == Administrator
      case _      => false
    })

  override def authorizeUserOrganisation(userOrganisation: UserOrganisation,
                                         role: OrganisationRole)(implicit
      ctx: ExecutionContext): Future[Boolean] =
    Future.successful((userOrganisation.role, role) match {
      case (x, y) => x == y || x == OrganisationAdministrator
      case _      => false
    })

  override def authorizeUserProject(userProject: UserProject,
                                    role: ProjectRole)(implicit
      ctx: ExecutionContext): Future[Boolean] =
    Future.successful((userProject.role, role) match {
      case (x, y) => x == y || x == ProjectAdministrator
      case _      => false
    })

  override def resolveUser(userReference: UserReference)(implicit
      context: ExecutionContext,
      dbSession: DBSession): Future[Option[User]] =
    userRepository.findByUserReference(userReference)

  override def authorizationFailed(request: RequestHeader)(implicit
      context: ExecutionContext): Future[Result] =
    Future.successful(Forbidden(Json.obj("message" -> "Unauthorized")))
}
