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

import scala.concurrent.ExecutionContext

import scala.concurrent.Future
import models._
import play.api.mvc.Action
import play.api.mvc.BodyParser
import play.api.mvc.Controller
import play.api.mvc.Request
import play.api.mvc.RequestHeader
import play.api.mvc.Result
import reactivemongo.bson.BSONObjectID
import play.api.Logger

class SecurityControllerMock(token: String = "", userId: UserId = UserId("someUserId"), authorized: Future[Boolean] = Future.successful(true), user: Option[User] = None, authorizationFailedResult: Result = null) extends SecurityComponentMock with Controller with Security with DefaultCacheProvider {
  override def HasToken[A](p: BodyParser[A] = parse.anyContent)(
    f: Subject => Request[A] => Future[Result])(implicit context: ExecutionContext): Action[A] = {
    Action.async(p) { implicit request =>
      f(Subject(token, userId))(request)
    }
  }

  override def HasRole[A, R <: Role](role: R, p: BodyParser[A] = parse.anyContent)(
    f: Subject => Request[A] => Future[Result])(implicit context: ExecutionContext): Action[A] = {
    Action.async(p) { implicit request =>
      f(Subject(token, userId))(request)
    }
  }
}