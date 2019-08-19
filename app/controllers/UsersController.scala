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

import core.DefaultCacheAware
import play.api.libs.json._
import play.api.mvc._
import repositories.{MongoSecurityRepositoryComponent, SecurityRepositoryComponent}

import scala.concurrent.ExecutionContext.Implicits.global

class UsersController {
  // Cake pattern
  this: SecurityRepositoryComponent with Controller with Security =>

  /**
   * Retrieves a logged in user if the authentication token is valid.
   *
   * If the token is invalid, [[HasToken]] does not invoke this function.
   *
   * returns The user in JSON format.
   */
  def authUser() = HasToken(parse.empty) { subject =>
    implicit request => {
      userRepository.findById(subject.userId).map(_.map {
        case (user) =>
          Ok(Json.toJson(user))
      }.getOrElse(BadRequest))
    }
  }
}

object UsersController extends UsersController  with Controller with Security with DefaultSecurityComponent with MongoSecurityRepositoryComponent with DefaultCacheAware
