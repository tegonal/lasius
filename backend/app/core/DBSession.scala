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
import reactivemongo.api.DB

import scala.concurrent.{ExecutionContext, Future}

class DBSession(private val session: DB,
                val db: DB,
                val withTransaction: Boolean) {
  def end()(implicit ec: ExecutionContext): Future[Unit] = for {
    _ <- if (withTransaction) db.commitTransaction() else Future.successful(())
    _ <- session.endSession()
  } yield ()

  def abort()(implicit ec: ExecutionContext): Future[Unit] = for {
    _ <- if (withTransaction) db.abortTransaction() else Future.successful(())
    _ <- session.endSession()
  } yield ()
}

object DBSession {
  def start(reactiveMongoApi: ReactiveMongoApi, withTransaction: Boolean)(
      implicit ec: ExecutionContext): Future[DBSession] = {
    for {
      db      <- reactiveMongoApi.database
      session <- db.startSession()
      dbWithTx <-
        if (withTransaction) session.startTransaction(None)
        else Future.successful(session)
    } yield new DBSession(session, dbWithTx, withTransaction)
  }
}
