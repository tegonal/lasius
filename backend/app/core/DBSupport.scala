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

package core

import play.modules.reactivemongo.ReactiveMongoApi

import scala.concurrent.{ExecutionContext, Future}

trait DBSupport {
  val reactiveMongoApi: ReactiveMongoApi
  val supportTransaction: Boolean

  protected def withinTransaction[R](f: DBSession => Future[R])(implicit
      executionContext: ExecutionContext): Future[R] = {
    withDBSession(withTransaction = supportTransaction)(f)
  }

  protected def withDBSession[R](withTransaction: Boolean = false)(
      f: DBSession => Future[R])(implicit
      executionContext: ExecutionContext): Future[R] = {
    (for {
      dbSession <- DBSession.start(reactiveMongoApi,
                                   withTransaction && supportTransaction)
      result <- f(dbSession)
        .flatMap(result => dbSession.end().map(_ => result))
        .recoverWith { case exception =>
          dbSession.abort().flatMap(_ => Future.failed(exception))
        }
    } yield result)
  }
}
