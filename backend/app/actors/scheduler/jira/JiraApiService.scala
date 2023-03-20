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

package actors.scheduler.jira

import java.net.URLEncoder

import actors.scheduler.{
  ApiServiceBase,
  ServiceAuthentication,
  ServiceConfiguration,
  WebServiceHelper
}
import com.google.inject.ImplementedBy
import javax.inject.Inject
import play.api._
import play.api.libs.json.{Json, Reads, _}
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait JiraApiService {

  /** Returns all projects which are visible for the currently logged in user.
    * If no user is logged in, it returns the list of projects that are visible
    * when using anonymous access.
    */
  def getAllProjects(expand: Option[String] = None)(implicit
      auth: ServiceAuthentication,
      executionContext: ExecutionContext): Future[Seq[JiraProject]]

  /** Returns all versions for the specified project. Results are paginated.
    */
  def getProjectVersions(projectIdOrKey: String,
                         startAt: Option[Integer] = None,
                         maxResults: Option[Integer] = None,
                         orderBy: Option[String] = None,
                         expand: Option[String] = None)(implicit
      auth: ServiceAuthentication,
      executionContext: ExecutionContext): Future[Seq[JiraVersion]]

  /** Contains a full representation of a the specified project's versions.
    */
  def getVersions(projectIdOrKey: String, expand: Option[String] = None)(
      implicit
      auth: ServiceAuthentication,
      executionContext: ExecutionContext): Future[Seq[JiraVersion]]

  /** Searches for issues using JQL.
    */
  def findIssues(jql: String,
                 startAt: Option[Integer] = None,
                 maxResults: Option[Integer] = None,
                 validateQuery: Option[Boolean] = None,
                 fields: Option[String] = Some("*navigatable"),
                 expand: Option[String] = None)(implicit
      auth: ServiceAuthentication,
      executionContext: ExecutionContext): Future[JiraSearchResult]
}

class JiraApiServiceImpl(override val ws: WSClient,
                         override val config: ServiceConfiguration)
    extends JiraApiService
    with ApiServiceBase {

  val allProjectsUrl     = "/rest/api/2/project?"
  val projectVersionsUrl = "/rest/api/2/project/%s/version?"
  val versionsUrl        = "/rest/api/2/project/%s/versions?"
  val findIssuesUrl      = "/rest/api/2/search?"

  def getAllProjects(expand: Option[String] = None)(implicit
      auth: ServiceAuthentication,
      executionContext: ExecutionContext): Future[Seq[JiraProject]] = {

    val params = getParamList(getParam("expand", expand))
    val url    = allProjectsUrl + params
    logger.debug(s"getAllProjects(expand:$expand, url:$url")
    getList[JiraProject](url).map(_._1)
  }

  def getProjectVersions(projectIdOrKey: String,
                         startAt: Option[Integer] = None,
                         maxResults: Option[Integer] = None,
                         orderBy: Option[String] = None,
                         expand: Option[String] = None)(implicit
      auth: ServiceAuthentication,
      executionContext: ExecutionContext): Future[Seq[JiraVersion]] = {
    val params = getParamList(getParam("startAt", startAt),
                              getParam("maxResults", maxResults),
                              getParam("orderBy", orderBy),
                              getParam("expand", expand))
    val url = projectVersionsUrl.format(projectIdOrKey) + params
    getList[JiraVersion](url).map(_._1)
  }

  def getVersions(projectIdOrKey: String, expand: Option[String] = None)(
      implicit
      auth: ServiceAuthentication,
      executionContext: ExecutionContext): Future[Seq[JiraVersion]] = {
    val params = getParamList(getParam("expand", expand))
    val url    = versionsUrl.format(projectIdOrKey) + params
    getList[JiraVersion](url).map(_._1)
  }

  def findIssues(jql: String,
                 startAt: Option[Integer] = None,
                 maxResults: Option[Integer] = None,
                 validateQuery: Option[Boolean] = None,
                 fields: Option[String] = Some("*navigatable"),
                 expand: Option[String] = None)(implicit
      auth: ServiceAuthentication,
      executionContext: ExecutionContext): Future[JiraSearchResult] = {
    val params = getParamList(
      getParam("jql", jql),
      getParam("startAt", startAt),
      getParam("maxResults", maxResults),
      getParam("validateQuery", validateQuery),
      getParam("fields", fields),
      getParam("expand", expand)
    )
    val url = findIssuesUrl + params
    getSingleValue[JiraSearchResult](url).map(_._1)
  }

}
