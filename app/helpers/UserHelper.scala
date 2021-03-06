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
package helpers

import models.Subject
import models.User
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import play.api.Logger
import repositories.UserRepository
import repositories.SecurityRepositoryComponent
import models.UserId

trait UserHelper {
  self: SecurityRepositoryComponent =>
  def withUser[R](errorResult: R)(f: User => Future[R])(implicit subject: Subject, context: ExecutionContext) = {
    forUser(subject.userId)(errorResult)(f)
  }

  def forUser[R](userId: UserId)(errorResult: R)(f: User => Future[R])(implicit context: ExecutionContext) = {
    userRepository.findById(userId) flatMap { o =>
      o.fold(Future.successful(errorResult)) { user =>
        f(user)
      }
    }
  }
}