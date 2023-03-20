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

package repositories

import com.google.inject.ImplementedBy
import core.DBSession
import models._
import play.api.Logging
import play.api.libs.json._
import reactivemongo.api.bson.collection.BSONCollection

import javax.inject.Inject
import scala.concurrent._

@ImplementedBy(classOf[JiraConfigMongoRepository])
trait JiraConfigRepository extends BaseRepository[JiraConfig, JiraConfigId] {

  def getJiraConfigurations(implicit
      dbSession: DBSession): Future[Seq[JiraConfig]]
}

class JiraConfigMongoRepository @Inject() (
    override implicit protected val executionContext: ExecutionContext)
    extends BaseReactiveMongoRepository[JiraConfig, JiraConfigId]
    with JiraConfigRepository
    with Logging {
  override protected[repositories] def coll(implicit
      dbSession: DBSession): BSONCollection =
    dbSession.db.collection[BSONCollection]("JiraConfig")

  def getJiraConfigurations(implicit
      dbSession: DBSession): Future[Seq[JiraConfig]] = {
    find(Json.obj()).map { configs =>
      logger.debug(s"Loaded jira configs:$configs")
      configs.map(_._1).toSeq
    }
  }
}
