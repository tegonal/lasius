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

import actors.TagCache.TagsUpdated
import actors.scheduler.{ServiceAuthentication, ServiceConfiguration}
import akka.actor._
import core.SystemServices
import models._
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

object GitlabTagParseWorker {
  def props(wsClient: WSClient,
            systemServices: SystemServices,
            config: ServiceConfiguration,
            settings: GitlabSettings,
            projectSettings: GitlabProjectSettings,
            auth: ServiceAuthentication,
            projectId: ProjectId): Props =
    Props(classOf[GitlabTagParseWorker],
          wsClient,
          systemServices,
          config,
          settings,
          projectSettings,
          auth,
          projectId)

  case object StartParsing
  case object Parse
}

class GitlabTagParseWorker(wsClient: WSClient,
                           systemServices: SystemServices,
                           config: ServiceConfiguration,
                           settings: GitlabSettings,
                           projectSettings: GitlabProjectSettings,
                           implicit val auth: ServiceAuthentication,
                           projectId: ProjectId)
    extends Actor
    with ActorLogging {
  import GitlabTagParseWorker._

  var cancellable: Option[Cancellable] = None
  val apiService      = new GitlabApiServiceImpl(wsClient, config)
  val defaultParams   = s"state=opened&order_by=created_at&sort=desc"
  val maxResults: Int = projectSettings.maxResults.getOrElse(500)

  val receive: Receive = { case StartParsing =>
    cancellable = Some(
      context.system.scheduler.scheduleOnce(0 milliseconds, self, Parse))
    context.become(parsing)
  }

  val parsing: Receive = { case Parse =>
    loadIssues(0, None)
      .map { result =>
        // fetched all results, notify
        if (log.isDebugEnabled) {
          val keys = result.map(i => s"#${i.iid}")
          log.debug(s"Parsed keys:$keys")
        }

        // assemble issue tags
        val tags = result.map(toGitlabIssueTag)
        systemServices.tagCache ! TagsUpdated[GitlabIssueTag](
          projectSettings.gitlabProjectId,
          projectId,
          tags)

        // handle new parsed issue keys
      }
      .andThen { case s =>
        // restart timer
        log.debug(s"andThen:restart time $s")
        cancellable = Some(
          context.system.scheduler
            .scheduleOnce(settings.checkFrequency milliseconds, self, Parse))
      }
  }

  private def toGitlabIssueTag(issue: GitlabIssue): GitlabIssueTag = {
    // create tag for milestone
    val milestoneTag = projectSettings.tagConfiguration.useMilestone match {
      case false => None
      case _     => issue.milestone.map(m => SimpleTag(TagId(m.title)))
    }

    val titleTag = projectSettings.tagConfiguration.useTitle match {
      case false => None
      case _     => Some(SimpleTag(TagId(issue.title)))
    }

    val labelTags = projectSettings.tagConfiguration.useLabels match {
      case false => Seq()
      case _ =>
        issue.labels
          .filterNot(projectSettings.tagConfiguration.labelFilter.contains(_))
          .map(l => SimpleTag(TagId(l)))
    }

    val tags =
      milestoneTag
        .map { m =>
          labelTags ++ titleTag
            .map(t => Seq(m, t))
            .getOrElse(Seq(m))
        }
        .getOrElse {
          titleTag.map(t => labelTags :+ t).getOrElse(labelTags)
        }

    val issueLink = issue.web_url

    GitlabIssueTag(
      TagId(
        projectSettings.projectKeyPrefix.getOrElse("") +
          issue.references.map(_.short).getOrElse(s"#${issue.iid}")),
      issue.project_id,
      Some(issue.title),
      tags,
      issueLink
    )
  }

  def loadIssues(
      offset: Int,
      max: Option[Int],
      lastResult: Set[GitlabIssue] = Set()): Future[Set[GitlabIssue]] = {
    val newMax = max.getOrElse(maxResults)
    issues(offset, newMax).flatMap { result =>
      val concat: Set[GitlabIssue] = lastResult ++ result.issues.toSet
      log.debug(
        s"loaded issues: maxResults:${result.totalNumberOfItems}, fetch count${concat.size}")
      if (concat.size >= result.totalNumberOfItems.getOrElse(Int.MaxValue)) {
        // fetched all results, notify
        Future.successful(concat)
      } else if (result.page.isDefined && result.page.get >= result.totalPages
          .getOrElse(Int.MaxValue)) {
        // fetched all pages
        Future.successful(concat)
      } else if (result.nextPage.isEmpty || 1 > result.nextPage.get) {
        // no next page
        Future.successful(concat)
      } else {
        // load next page
        loadIssues(result.nextPage.get, max, concat)
      }
    }
  }

  def issues(offset: Int, max: Int): Future[GitlabIssuesSearchResult] = {
    log.debug(
      s"Parse issues projectId=${projectId.value}, project=${projectSettings.gitlabProjectId}, offset:$offset, max:$max")
    val query = projectSettings.params.getOrElse(defaultParams)
    apiService
      .findIssues(projectSettings.gitlabProjectId,
                  query,
                  Some(offset),
                  Some(max))
  }

  override def postStop(): Unit = {
    cancellable.map(c => c.cancel())
    cancellable = None
    super.postStop()
  }
}
