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

import play.api._

import play.api.mvc._
import play.api.libs.json._
import models._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import play.api.mvc.Result
import play.api.mvc.RequestHeader
import repositories.SecurityRepositoryComponent
import helpers.UserHelper
import repositories.MongoSecurityRepositoryComponent
import play.api.libs.json.Json.toJsFieldJsValueWrapper

trait AuthConfig {
  /**
   * Map usertype to permission role.
   */
  def authorize(user: User, role: Role)(implicit ctx: ExecutionContext): Future[Boolean]

  /**
   * Resolve user based on bson object id
   */
  def resolveUser(userId: UserId)(implicit context: ExecutionContext): Future[Option[User]]

  /**
   * Defined handling of authorizationfailed
   */
  def authorizationFailed(request: RequestHeader)(implicit context: ExecutionContext): Future[Result]
}

trait DefaultAuthConfig extends AuthConfig with UserHelper {
  self: Controller with SecurityRepositoryComponent =>
  /**
   * Map usertype to permission role.
   */
  def authorize(user: User, role: Role)(implicit ctx: ExecutionContext) = Future.successful((user.role, role) match {
    case (x, y) => x == y || x == Administrator
    case _ => false
  })

  def resolveUser(userId: UserId)(implicit context: ExecutionContext): Future[Option[User]] = {
    userRepository.findById(userId)
  }

  def authorizationFailed(request: RequestHeader)(implicit context: ExecutionContext): Future[Result] = {
    Future.successful(Unauthorized(Json.obj("message" -> "Unauthorized")))
  }
}

object DefaultAuthConfig extends DefaultAuthConfig with MongoSecurityRepositoryComponent with Controller