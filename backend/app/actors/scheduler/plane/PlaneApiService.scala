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

package actors.scheduler.plane

import actors.scheduler.{
  ApiServiceBase,
  ServiceAuthentication,
  ServiceConfiguration
}
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

trait PlaneApiService {

  /** Searches for issues using post params.
    */
  def findIssues(projectId: String,
                 query: String,
                 page: Option[Int] = None,
                 maxResults: Option[Int] = None)(implicit
      auth: ServiceAuthentication,
      executionContext: ExecutionContext): Future[PlaneIssuesSearchResult]
}

class PlaneApiServiceImpl(override val ws: WSClient,
                          override val config: ServiceConfiguration)
    extends PlaneApiService
    with ApiServiceBase {

  val findIssuesUrl = s"/api/v1/workspaces/tegonal-intern/projects/%s/issues/"

  def findIssues(projectId: String,
                 paramString: String,
                 page: Option[Int] = None,
                 maxResults: Option[Int] = None)(implicit
      auth: ServiceAuthentication,
      executionContext: ExecutionContext): Future[PlaneIssuesSearchResult] = {

    val params = getParamList(Some(paramString),
                              getParam("cursor", """${maxResults}:${page}:0"""),
                              getParam("per_page", maxResults))

    val url = findIssuesUrl.format(projectId) + params
    getList[PlaneIssue](url).map { pair =>
      PlaneIssuesSearchResult(
        pair._1,
        pair._2
          .get("total_results")
          .flatMap(_.headOption.flatMap(v => Try(v.toInt).toOption)),
        pair._2
          .get("total_pages")
          .flatMap(_.headOption.flatMap(v => Try(v.toInt).toOption)),
        maxResults,
        pair._2
          .get("count")
          .flatMap(_.headOption.flatMap(v => Try(v.toInt).toOption)),
        pair._2
          .get("next_page_results")
          .flatMap(_.headOption.flatMap(v => Try(v.toBoolean).toOption)),
        pair._2
          .get("prev_page_results")
          .flatMap(_.headOption.flatMap(v => Try(v.toBoolean).toOption))
      )
    }
  }
}
