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

package helpers

import core.DBSession
import models.UserId.UserReference
import models.{EntityReference, Subject, User, UserId}
import repositories.SecurityRepositoryComponent

import scala.concurrent.{ExecutionContext, Future}

trait UserHelper {
  self: SecurityRepositoryComponent =>
  def withUser[R](errorResult: R)(f: User => Future[R])(implicit
      subject: Subject[_],
      dbSession: DBSession,
      context: ExecutionContext): Future[R] = {
    forUser(subject.userReference)(errorResult)(f)
  }

  def forUser[R](userReference: UserReference)(errorResult: R)(
      f: User => Future[R])(implicit
      context: ExecutionContext,
      dbSession: DBSession): Future[R] = {
    userRepository.findByUserReference(userReference).flatMap { o =>
      o.fold(Future.successful(errorResult)) { user =>
        f(user)
      }
    }
  }
}
