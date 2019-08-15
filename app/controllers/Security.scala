/*   __                          __                                          *\
*   / /____ ___ ____  ___  ___ _/ /       lasius                      *
*  / __/ -_) _ `/ _ \/ _ \/ _ `/ /        contributed by tegonal              *
*  \__/\__/\_, /\___/_//_/\_,_/_/         http://tegonal.com/                 *
*         /___/                                                               *
*                                                                             *
* This program is free software: you can redistribute it and/or modify it     *
* under the terms of the GNU General Public License as published by    *
* the Free Software Foundation, either version 3 of the License,              *
* or (at your option) any later version.                                      *
*                                                                             *
* This program is distributed in the hope that it will be useful, but         *
* WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY  *
* or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for *
* more details.                                                               *
*                                                                             *
* You should have received a copy of the GNU General Public License along     *
* with this program. If not, see http://www.gnu.org/licenses/                 *
*                                                                             *
\*                                                                           */
package controllers

import models.{Role, Subject, UserId}
import play.api.Logger
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

/**
 * Security actions that should be used by all controllers that need to protect their actions.
 * Can be composed to fine-tune access control.
 */
trait SecurityComponent {
  val authConfig: AuthConfig
}

trait Security {
  self: Controller with SecurityComponent with CacheAware=>

  val AuthTokenHeader = "X-XSRF-TOKEN"
  val AuthTokenCookieKey = "XSRF-TOKEN"
  val AuthTokenUrlKey = "auth"

  /**
   * Checks that the token is:
   * - present in the cookie header of the request,
   * - either in the header or in the query string,
   * - matches a token already stored in the play cache
   */
  def HasToken[A](p: BodyParser[A] = parse.anyContent)(
    f: Subject => Request[A] => Future[Result])(implicit context: ExecutionContext): Action[A] = {
    Action.async(p) { implicit request =>
      checkToken match {
        case Left(subject) => {
          f(subject)(request)
        }
        case Right(result) => {
          Future.successful(result)
        }
      }
    }
  }

  /**
   * check is user has a token
   */
  def checkToken[A]()(implicit request: RequestHeader, context: ExecutionContext): Either[Subject, Result] = {
    request.cookies.get(AuthTokenCookieKey) map { xsrfTokenCookie =>
      val maybeToken = request.headers.get(AuthTokenHeader).orElse(request.getQueryString(AuthTokenUrlKey))
      Logger.debug("Token from headers:" + maybeToken)
      maybeToken.flatMap { token =>
        Logger.debug(s"Check security token in cache:$token, " + cache.get(token))
        cache.get[UserId](token) map { userId =>
          Logger.debug(s"Found userId: $userId")
          if (xsrfTokenCookie.value.equals(token)) {
            val subject = Subject(token, userId)
            Left(subject)
          } else {
            Right(Unauthorized(Json.obj("message" -> "Invalid Token")))
          }
        }
      } getOrElse {
        Right(Unauthorized(Json.obj("message" -> "No Token")))
      }
    } getOrElse {
      Right(Unauthorized(Json.obj("message" -> "Invalid XSRF Token cookie")))
    }
  }

  def HasRole[A, R <: Role](role: R, p: BodyParser[A] = parse.anyContent)(
    f: Subject => Request[A] => Future[Result])(implicit context: ExecutionContext): Action[A] = {
    HasToken(p) { implicit subject =>
      implicit request => {
        authConfig.resolveUser(subject.userId) flatMap {
          case Some(user) =>
            authConfig.authorize(user, role) flatMap {
              case true => checked(f(subject)(request))
              case _ => authConfig.authorizationFailed(request)
            } recoverWith {
              case e => {
                Logger.error("Got error executing conctroller function: " + e.getMessage)
                Future.successful(InternalServerError(Json.obj("message" -> e.getMessage)))
              }
            }
          case _ => authConfig.authorizationFailed(request)
        }
      }
    }
  }

  def checked[A](f: => Future[Result])(implicit context: ExecutionContext): Future[Result] = {
    val r = f
    r.recoverWith {
      case t =>
        Logger.debug(s"Error", t)
        Future.successful(BadRequest(t.getMessage))
    }
  }

  def checkSSOAlive() = {

  }
}

trait DefaultSecurityComponent extends SecurityComponent {
  val authConfig = DefaultAuthConfig
}
