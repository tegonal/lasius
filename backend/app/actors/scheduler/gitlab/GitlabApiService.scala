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

package actors.scheduler.gitlab

import actors.scheduler.{
  ApiServiceBase,
  ServiceAuthentication,
  ServiceConfiguration
}
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

trait GitlabApiService {

  /** Searches for issues using post params.
    */
  def findIssues(projectId: String,
                 query: String,
                 page: Option[Int] = None,
                 maxResults: Option[Int] = None)(implicit
      auth: ServiceAuthentication,
      executionContext: ExecutionContext): Future[GitlabIssuesSearchResult]
}

class GitlabApiServiceImpl(override val ws: WSClient,
                           override val config: ServiceConfiguration)
    extends GitlabApiService
    with ApiServiceBase {

  val findIssuesUrl = s"/projects/%s/issues?"

  def findIssues(projectId: String,
                 paramString: String,
                 page: Option[Int] = None,
                 maxResults: Option[Int] = None)(implicit
      auth: ServiceAuthentication,
      executionContext: ExecutionContext): Future[GitlabIssuesSearchResult] = {

    val params = getParamList(Some(paramString),
                              getParam("page", page),
                              getParam("per_page", maxResults))

    val url = findIssuesUrl.format(projectId) + params
    getList[GitlabIssue](url).map { pair =>
      GitlabIssuesSearchResult(
        pair._1,
        pair._2
          .get("X-Total")
          .flatMap(_.headOption.flatMap(v => Try(v.toInt).toOption)),
        pair._2
          .get("X-Total-Pages")
          .flatMap(_.headOption.flatMap(v => Try(v.toInt).toOption)),
        pair._2
          .get("X-Per-Page")
          .flatMap(_.headOption.flatMap(v => Try(v.toInt).toOption)),
        pair._2
          .get("X-Page")
          .flatMap(_.headOption.flatMap(v => Try(v.toInt).toOption)),
        pair._2
          .get("X-Next-Page")
          .flatMap(_.headOption.flatMap(v => Try(v.toInt).toOption)),
        pair._2
          .get("X-Prev-Page")
          .flatMap(_.headOption.flatMap(v => Try(v.toInt).toOption))
      )
    }
  }
}
