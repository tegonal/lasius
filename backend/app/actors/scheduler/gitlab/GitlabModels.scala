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

import org.joda.time.DateTime
import play.api.libs.json._

import java.util.Date

case class GitlabMilestone(
    due_date: Option[Date],
    project_id: Int,
    state: String,
    description: Option[String],
    iid: Int,
    id: Int,
    title: String,
    created_at: DateTime,
    updated_at: DateTime,
    closed_at: Option[DateTime]
)

case class GitlabUser(
    state: String,
    web_url: Option[String],
    avatar_url: Option[String],
    username: String,
    id: Int,
    name: String
)

case class GitlabReference(
    short: String,
    relative: String,
    full: String
)

case class GitlabTimeslot(
    time_estimate: Option[Int],
    total_time_spent: Option[Int],
    human_time_estimate: Option[String],
    human_total_time_spent: Option[String]
)

case class GitlabLinks(
    self: String,
    notes: String,
    award_emoji: String,
    project: String
)

case class GitlabTaskCompletionStatus(
    count: Int,
    completed_count: Int
)

case class GitlabIssue(
    project_id: Int,
    milestone: Option[GitlabMilestone],
    author: GitlabUser,
    description: Option[String],
    state: String,
    iid: Int,
    assignees: Seq[GitlabUser],
    assignee: Option[GitlabUser],
    labels: Seq[String],
    // remove to not exceed limit of 22 fields
    // upvotes: Int,
    // downvotes: Int,
    // merge_requests_count: Int,
    id: Int,
    title: String,
    created_at: DateTime,
    updated_at: DateTime,
    closed_at: Option[DateTime],
    closed_by: Option[GitlabUser],
    // subscribed: Option[Boolean],
    // user_notes_count: Int,
    due_date: Option[Date],
    web_url: String,
    references: Option[GitlabReference],
    time_stats: Option[GitlabTimeslot],
    confidential: Option[Boolean],
    // discussion_locked: Option[Boolean],
    _links: Option[GitlabLinks],
    task_completion_status: Option[GitlabTaskCompletionStatus]
)

case class GitlabIssuesSearchResult(
    issues: Seq[GitlabIssue],
    totalNumberOfItems: Option[Int],
    totalPages: Option[Int],
    perPage: Option[Int],
    page: Option[Int],
    nextPage: Option[Int],
    prevPage: Option[Int]
)

object GitlabMilestone {
  import models.BaseFormat._
  implicit val jsonFormat: Format[GitlabMilestone] =
    Json.format[GitlabMilestone]
}
object GitlabUser {
  implicit val jsonFormat: Format[GitlabUser] = Json.format[GitlabUser]
}
object GitlabReference {
  implicit val jsonFormat: Format[GitlabReference] =
    Json.format[GitlabReference]
}
object GitlabTimeslot {
  implicit val jsonFormat: Format[GitlabTimeslot] = Json.format[GitlabTimeslot]
}
object GitlabLinks {
  implicit val jsonFormat: Format[GitlabLinks] = Json.format[GitlabLinks]
}
object GitlabTaskCompletionStatus {
  implicit val jsonFormat: Format[GitlabTaskCompletionStatus] =
    Json.format[GitlabTaskCompletionStatus]
}
object GitlabIssue {
  import models.BaseFormat._
  implicit val jsonFormat: Format[GitlabIssue] = Json.format[GitlabIssue]
}
object GitlabIssuesSearchResult {
  implicit val jsonFormat: Format[GitlabIssuesSearchResult] =
    Json.format[GitlabIssuesSearchResult]
}
