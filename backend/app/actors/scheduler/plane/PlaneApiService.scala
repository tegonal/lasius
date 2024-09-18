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

import actors.scheduler.jira.JiraVersion
import actors.scheduler.{
  ApiServiceBase,
  ServiceAuthentication,
  ServiceConfiguration
}
import akka.actor.ActorLogging
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

  def getLabels(projectId: String)(implicit
      auth: ServiceAuthentication,
      executionContext: ExecutionContext): Future[Seq[PlaneLabel]]
}

class PlaneApiServiceImpl(override val ws: WSClient,
                          override val config: ServiceConfiguration)
    extends PlaneApiService
    with ApiServiceBase {

  val findIssuesUrl = s"/api/v1/workspaces/tegonal-intern/projects/%s/issues/?"
  val fetchLabelUrl = s"/api/v1/workspaces/tegonal-intern/projects/%s/labels/?"

  def getLabels(projectId: String)(implicit
      auth: ServiceAuthentication,
      executionContext: ExecutionContext): Future[Seq[PlaneLabel]] = {
    val params = getParamList(getParam("per_page", 100))
    val url    = fetchLabelUrl.format(projectId) + params
    getList[PlaneLabel](url).map(_._1)
  }

  def findIssues(projectId: String,
                 paramString: String,
                 page: Option[Int] = None,
                 maxResults: Option[Int] = None)(implicit
      auth: ServiceAuthentication,
      executionContext: ExecutionContext): Future[PlaneIssuesSearchResult] = {

    val currentPage       = page.getOrElse(0)
    val currentMaxResults = maxResults.getOrElse(100)

    val params = getParamList(
      Some(paramString),
      getParam("cursor", s"${currentMaxResults}:${currentPage}:0"),
      getParam("per_page", currentMaxResults))

    val url = findIssuesUrl.format(projectId) + params
    logger.debug(s"findIssues: $url")
    getSingleValue[PlaneIssueWrapper](url).map { pair =>
      PlaneIssuesSearchResult(
        pair._1.results,
        Some(pair._1.total_results),
        Some(pair._1.total_pages),
        maxResults,
        Some(pair._1.count),
        Some(pair._1.next_page_results),
        Some(pair._1.prev_page_results)
      )
    }
  }
}
