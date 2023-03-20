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

import core.Validation.ValidationFailedException
import core.{CacheAware, DBSession, DBSupport}
import models.OrganisationId.OrganisationReference
import models.UserId.UserReference
import models.{
  EntityReference,
  OrganisationAdministrator,
  OrganisationId,
  OrganisationMember,
  OrganisationRole,
  ProjectId,
  ProjectRole,
  Subject,
  User,
  UserId,
  UserOrganisation,
  UserProject,
  UserRole
}
import play.api.Logging
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.json.{Json, Reads}
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

/** Security actions that should be used by all controllers that need to protect
  * their actions. Can be composed to fine-tune access control.
  */
trait SecurityComponent {
  val authConfig: AuthConfig
}

trait Security extends Logging {
  self: BaseController with SecurityComponent with CacheAware with DBSupport =>

  val AuthTokenHeader    = "X-XSRF-TOKEN"
  val AuthTokenCookieKey = "XSRF-TOKEN"
  val AuthTokenUrlKey    = "auth"

  def HasToken[A](withinTransaction: Boolean)(
      f: DBSession => Subject => Request[A] => Future[Result])(implicit
      context: ExecutionContext,
      reader: Reads[A]): Action[A] = {
    HasToken(parse.json[A], withinTransaction)(f)
  }

  /** Checks that the token is:
    *   - present in the cookie header of the request,
    *   - either in the header or in the query string,
    *   - matches a token already stored in the play cache
    */
  def HasToken[A](p: BodyParser[A], withinTransaction: Boolean)(
      f: DBSession => Subject => Request[A] => Future[Result])(implicit
      context: ExecutionContext): Action[A] = {
    Action.async(p) { implicit request =>
      withDBSession(withinTransaction) { implicit dbSession =>
        checkToken().flatMap {
          case Left(subject) =>
            f(dbSession)(subject)(request)
          case Right(result) =>
            Future.successful(result)
        }
      }
    }
  }

  /** check is user has a token
    */
  def checkToken[A]()(implicit
      request: RequestHeader,
      context: ExecutionContext): Future[Either[Subject, Result]] = {
    request.cookies
      .get(AuthTokenCookieKey)
      .map { xsrfTokenCookie =>
        val maybeToken = request.headers
          .get(AuthTokenHeader)
          .orElse(request.getQueryString(AuthTokenUrlKey))
        logger.debug("Token from headers:" + maybeToken)
        maybeToken
          .map { token =>
            logger.debug(
              s"Check security token in cache:$token, " + cache.sync.get(token))
            cache.get[UserReference](token).map {
              _.map { userReference =>
                logger.debug(s"Found userId: ${userReference.id}")
                if (xsrfTokenCookie.value.equals(token)) {
                  val subject = Subject(token, userReference)
                  Left(subject)
                } else {
                  Right(Unauthorized(Json.obj("message" -> "Invalid Token")))
                }
              }.getOrElse(Right(
                Unauthorized(Json.obj("message" -> "No Token"))))
            }
          }
          .getOrElse {
            Future.successful(
              Right(Unauthorized(Json.obj("message" -> "No Token"))))
          }
      }
      .getOrElse {
        Future.successful(
          Right(
            Unauthorized(Json.obj("message" -> "Invalid XSRF Token cookie"))))
      }
  }

  def HasUserRole[A, R <: UserRole](role: R, withinTransaction: Boolean)(
      f: DBSession => Subject => User => Request[A] => Future[Result])(implicit
      context: ExecutionContext,
      reader: Reads[A]): Action[A] = {
    HasUserRole(role, parse.json[A], withinTransaction)(f)
  }

  def HasUserRole[A, R <: UserRole](role: R,
                                    p: BodyParser[A],
                                    withinTransaction: Boolean)(
      f: DBSession => Subject => User => Request[A] => Future[Result])(implicit
      context: ExecutionContext): Action[A] = {
    HasToken(p, withinTransaction) {
      implicit dbSession => implicit subject => implicit request =>
        {
          checked(authConfig.resolveUser(subject.userReference).flatMap {
            case Some(user) if user.active =>
              authConfig
                .authorizeUser(user, role)
                .flatMap {
                  case true => f(dbSession)(subject)(user)(request)
                  case _    => authConfig.authorizationFailed(request)
                }
            case _ => authConfig.authorizationFailed(request)
          })
        }
    }
  }

  def HasOptionalOrganisationRole[A, R <: OrganisationRole](
      user: User,
      maybeOrganisation: Option[OrganisationId],
      role: R)(f: Option[UserOrganisation] => Future[Result])(implicit
      context: ExecutionContext,
      dbSession: DBSession,
      request: Request[A]): Future[Result] = {
    maybeOrganisation
      .fold(f(None))(orgId =>
        HasOrganisationRole(user, orgId, role)(userOrg => f(Some(userOrg))))
  }

  def HasOrganisationRole[A, R <: OrganisationRole](user: User,
                                                    orgId: OrganisationId,
                                                    role: R)(
      f: UserOrganisation => Future[Result])(implicit
      context: ExecutionContext,
      dbSession: DBSession,
      request: Request[A]): Future[Result] = {
    checked(user.organisations.find(_.organisationReference.id == orgId) match {
      case Some(userOrganisation) =>
        authConfig
          .authorizeUserOrganisation(userOrganisation, role)
          .flatMap {
            case true => f(userOrganisation)
            case _    => authConfig.authorizationFailed(request)
          }
      case _ => authConfig.authorizationFailed(request)
    })
  }

  def HasProjectRole[A, R <: ProjectRole](userOrganisation: UserOrganisation,
                                          projectId: ProjectId,
                                          role: R)(
      f: UserProject => Future[Result])(implicit
      context: ExecutionContext,
      dbSession: DBSession,
      request: Request[A]): Future[Result] = {
    checked(
      userOrganisation.projects.find(_.projectReference.id == projectId) match {
        case Some(userProject) =>
          authConfig
            .authorizeUserProject(userProject, role)
            .flatMap {
              case true => f(userProject)
              case _    => authConfig.authorizationFailed(request)
            }
        case _ => authConfig.authorizationFailed(request)
      })
  }

  /** This helper method checks if a user has at least the role
    * OrganisationMember and is either OrganisationAdministrator or has provided
    * project role
    */
  protected def isOrgAdminOrHasProjectRoleInOrganisation[A](
      user: User,
      orgId: OrganisationId,
      projectId: ProjectId,
      projectRole: ProjectRole)(f: UserOrganisation => Future[Result])(implicit
      context: ExecutionContext,
      dbSession: DBSession,
      request: Request[A]): Future[Result] = {
    HasOrganisationRole(user, orgId, OrganisationMember) { userOrg =>
      // either org admin or project admin
      val projectToCheck = if (userOrg.role == OrganisationAdministrator) {
        None
      } else {
        Some(projectId)
      }
      HasOptionalProjectRole(userOrg, projectToCheck, projectRole) { _ =>
        f(userOrg)
      }
    }
  }

  def HasOptionalProjectRole[A, R <: ProjectRole](
      userOrganisation: UserOrganisation,
      maybeProjectId: Option[ProjectId],
      role: R)(f: Option[UserProject] => Future[Result])(implicit
      context: ExecutionContext,
      dbSession: DBSession,
      request: Request[A]): Future[Result] = {
    maybeProjectId
      .fold(f(None))(projectId =>
        HasProjectRole(userOrganisation, projectId, role)(userProject =>
          f(Some(userProject))))
  }

  def checked(f: => Future[Result])(implicit
      context: ExecutionContext): Future[Result] = {
    f.recoverWith {
      case e: ValidationFailedException =>
        logger.debug(s"Validation errror", e)
        Future.successful(
          BadRequest(
            Option(e.getMessage()).getOrElse(e.getClass.getSimpleName)))
      case e =>
        logger.error(s"Unknown Error", e)
        Future.successful(
          InternalServerError(
            Option(e.getMessage()).getOrElse(e.getClass.getSimpleName)))
    }
  }

  def checkSSOAlive(): Unit = {}
}
