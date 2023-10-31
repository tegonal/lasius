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

import controllers.Security._
import core.Validation.ValidationFailedException
import core.{CacheAware, DBSession, DBSupport}
import models.UserId.UserReference
import models._
import org.apache.commons.codec.binary.Base64
import play.api.Logging
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.json.{Json, Reads}
import play.api.mvc.Results.Unauthorized
import play.api.mvc._
import scalaoauth2.provider.OAuth2ProviderActionBuilders

import java.util.UUID
import scala.concurrent.Future.successful
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

/** Security actions that should be used by all controllers that need to protect
  * their actions. Can be composed to fine-tune access control.
  */
trait SecurityComponent {
  val authConfig: AuthConfig
}

trait Security extends Logging with OAuth2ProviderActionBuilders {
  self: BaseController with SecurityComponent with CacheAware with DBSupport =>

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
        checkAuthOrAccessToken().flatMap {
          case Right(subject) =>
            f(dbSession)(subject)(request)
          case Left(result) =>
            successful(result)
        }
      }
    }
  }

  /** check is user has an auth or an access-token
    */
  private def checkAuthOrAccessToken()(implicit
      request: RequestHeader,
      context: ExecutionContext,
      dbSession: DBSession): Future[Either[Result, Subject]] = {
    checkAuthToken().flatMap {
      case Right(result) => successful(Right(result))
      case Left(_)       => checkAccessToken()
    }
  }

  /** check auth token by validating xsrf-token cookie with xsrf-token provided
    * either in header or as query parameter used for websocket authentication.
    * Perform user reference lookup within cache or db
    */
  def checkOneTimeAuthToken()(implicit
      request: RequestHeader,
      context: ExecutionContext,
      dbSession: DBSession): Future[Either[Result, Subject]] =
    request.cookies
      .get(AuthTokenCookieKey)
      .map { xsrfTokenCookie =>
        val maybeOneTimeToken =
          request.getQueryString(AuthOneTimeTokenQueryParamKey)
        logger.debug("Nonce auth Token from headers:" + maybeOneTimeToken)
        maybeOneTimeToken
          .map { oneTimeToken =>
            nonceAuthTokenCache
              .get[String](oneTimeToken)
              .flatMap {
                _.map { authToken =>
                  if (xsrfTokenCookie.value.equals(oneTimeToken)) {
                    resolveAuthTokenFromCacheOrDbCache(authToken)
                  } else {
                    successful(Left(
                      Unauthorized(Json.obj("message" -> "Invalid Token"))))
                  }
                }.getOrElse {
                  successful(
                    Left(Unauthorized(Json.obj("message" -> "No Token"))))
                }

              }
          }
          .getOrElse {
            successful(Left(Unauthorized(Json.obj("message" -> "No Token"))))
          }
      }
      .getOrElse {
        successful(
          Left(
            Unauthorized(Json.obj("message" -> "Invalid XSRF Token cookie"))))
      }

  /** check auth token by validating xsrf-token cookie with xsrf-token provided
    * in the request header. Perform user reference lookup within cache or db
    */
  private def checkAuthToken()(implicit
      request: RequestHeader,
      context: ExecutionContext,
      dbSession: DBSession): Future[Either[Result, Subject]] = {
    request.cookies
      .get(AuthTokenCookieKey)
      .map { xsrfTokenCookie =>
        val maybeToken = request.headers
          .get(AuthTokenHeader)
        logger.debug("Token from headers:" + maybeToken)
        maybeToken
          .map { authToken =>
            if (xsrfTokenCookie.value.equals(authToken)) {
              logger.debug(
                s"Check security token in cache:$authToken, " + authTokenCache.sync
                  .get(authToken))
              resolveAuthTokenFromCacheOrDbCache(authToken)
            } else {
              successful(Security.invalidTokenResult)
            }
          }
          .getOrElse(successful(Security.missingTokenResult))
      }
      .getOrElse(successful(Security.missingTokenResult))
  }

  private def resolveAuthTokenFromCacheOrDbCache(authToken: String)(implicit
      context: ExecutionContext,
      dbSession: DBSession): Future[Either[Result, Subject]] =
    authTokenCache
      .get[UserReference](authToken)
      .flatMap {
        _.map { userReference =>
          logger.debug(s"Found userId: ${userReference.id}")
          successful(Some(userReference))
        }.getOrElse {
          // lookup in db
          authConfig.resolveUserByAuthToken(authToken).map {
            _.map { userReference =>
              // update token in cache, for a max of 1 day, after this, token needs to be renewed
              authTokenCache.set(authToken, userReference, system)

              userReference
            }
          }
        }
      }
      .map {
        _.map { userReference =>
          val subject = Subject(authToken, userReference)

          Right(subject)
        }.getOrElse(Security.missingTokenResult)
      }

  /** check access token by querying a special access-token header with the
    * base64 encoded accessTokenKey::accessTokenSecret pair. Successful if
    * accessTokenKey can be resolved within accessTokenCache or looked up with a
    * matching accessTokenKey/accessTokenSecret pair
    */
  private def checkAccessToken()(implicit
      request: RequestHeader,
      context: ExecutionContext,
      dbSession: DBSession): Future[Either[Result, Subject]] = {
    request.headers
      .get(AccessTokenHeader)
      .map { accessTokenPair =>
        val pair =
          new String(Base64.decodeBase64(accessTokenPair.getBytes("utf-8")))
        pair.split("::").toList match {
          case accessTokenKey :: accessTokenSecret :: Nil =>
            accessTokenCache
              .get[UserReference](accessTokenKey)
              .flatMap {
                _.map { userReference =>
                  successful(Some(userReference))
                }
                  .getOrElse {
                    authConfig
                      .resolveUserByAccessToken(
                        AccessTokenId(UUID.fromString(accessTokenKey)),
                        accessTokenSecret
                      )
                      .map {
                        _.map { userReference =>
                          // update token in cache for a short period of time
                          // to reduce load to db
                          accessTokenCache.set(accessTokenKey,
                                               userReference,
                                               1 minute)
                          userReference
                        }

                      }
                  }
              }
              .map {
                _.map { userReference =>
                  val subject = Subject(accessTokenKey, userReference)

                  Right(subject)
                }.getOrElse(missingTokenResult)
              }
          case _ => successful(invalidTokenResult)
        }
      }
      .getOrElse(successful(missingTokenResult))
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
        logger.debug(s"Validation error", e)
        successful(
          BadRequest(Option(e.getMessage).getOrElse(e.getClass.getSimpleName)))
      case e =>
        logger.error(s"Unknown Error", e)
        successful(
          InternalServerError(
            Option(e.getMessage).getOrElse(e.getClass.getSimpleName)))
    }
  }
}

object Security {
  private val invalidTokenResult = Left(
    Unauthorized(Json.obj("message" -> "Invalid Token")))

  private val missingTokenResult = Left(
    Unauthorized(Json.obj("message" -> "No Token")))

  val AuthTokenHeader               = "X-XSRF-TOKEN"
  val AccessTokenHeader             = "X-ACS-TOKEN"
  val AuthTokenCookieKey            = "XSRF-TOKEN"
  val AuthOneTimeTokenQueryParamKey = "token"
}
