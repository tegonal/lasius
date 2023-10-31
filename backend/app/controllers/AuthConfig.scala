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
import com.typesafe.config.Config
import core.{DBSession, SystemServices}
import helpers.UserHelper
import models.UserId.UserReference
import models._
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.json._
import play.api.mvc._
import repositories.{SecurityRepositoryComponent, UserRepository}

import javax.inject.Inject
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.DurationConverters._

@ImplementedBy(classOf[DefaultAuthConfig])
trait AuthConfig {

  // max time an auth token lived in the cache to reduce load to db
  val authTokenMaxCacheTime: Duration
  // time an auth token expires and needs to be renewed
  val authTokenExpiresAfter: Duration
  // max time the auth token can still be renewed after it was expired
  val authTokenMaxRenewTime: Duration
  // max time an access token lives in the cache to reduce load to db
  val accessTokenMaxCacheTime: Duration
  // max expiration time an access token can be configured to
  val accessTokenMaxExpirationTime: Duration

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

  /** Lookup user by token
    */
  def resolveUserByAuthToken(token: String)(implicit
      context: ExecutionContext,
      dbSession: DBSession): Future[Option[UserReference]]

  def resolveUserByAccessToken(accessTokenId: AccessTokenId,
                               tokenSecret: String)(implicit
      context: ExecutionContext,
      dbSession: DBSession): Future[Option[UserReference]]

  /** Defined handling of authorizationfailed
    */
  def authorizationFailed(request: RequestHeader)(implicit
      context: ExecutionContext): Future[Result]
}

class DefaultAuthConfig @Inject() (controllerComponents: ControllerComponents,
                                   systemServices: SystemServices,
                                   override val userRepository: UserRepository,
                                   config: Config)
    extends AbstractController(controllerComponents)
    with AuthConfig
    with UserHelper
    with SecurityRepositoryComponent {

  val authTokenMaxCacheTime: Duration =
    config.getDuration("auth_token.max_cache_time").toScala
  val authTokenExpiresAfter: Duration =
    config.getDuration("auth_token.expires_after").toScala
  val authTokenMaxRenewTime: Duration =
    config.getDuration("auth_token.max_renew_time").toScala
  val accessTokenMaxCacheTime: Duration =
    config.getDuration("access_token.max_cache_time").toScala
  val accessTokenMaxExpirationTime: Duration =
    config.getDuration("access_token.max_expiration_time").toScala

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

  override def resolveUserByAuthToken(token: String)(implicit
      context: ExecutionContext,
      dbSession: DBSession): Future[Option[UserReference]] =
    userRepository.findByAuthToken(token)

  override def resolveUserByAccessToken(accessTokenId: AccessTokenId,
                                        tokenSecret: String)(implicit
      context: ExecutionContext,
      dbSession: DBSession): Future[Option[UserReference]] =
    accessTokenRepository.resolveAndValidateToken(accessTokenId, tokenSecret)

  override def authorizationFailed(request: RequestHeader)(implicit
      context: ExecutionContext): Future[Result] =
    Future.successful(Forbidden(Json.obj("message" -> "Unauthorized")))
}
