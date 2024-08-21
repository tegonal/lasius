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

import org.joda.time.DateTime
import play.api.libs.json._

import java.util.Date

case class PlaneMilestone(
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

case class PlaneUser(
    state: String,
    web_url: Option[String],
    avatar_url: Option[String],
    username: String,
    id: Int,
    name: String
)

case class PlaneReference(
    short: String,
    relative: String,
    full: String
)

case class PlaneTimeslot(
    time_estimate: Option[Int],
    total_time_spent: Option[Int],
    human_time_estimate: Option[String],
    human_total_time_spent: Option[String]
)

case class PlaneLinks(
    self: String,
    notes: String,
    award_emoji: String,
    project: String
)

case class PlaneTaskCompletionStatus(
    count: Int,
    completed_count: Int
)

case class PlaneIssue(
    project_id: Int,
    milestone: Option[PlaneMilestone],
    author: PlaneUser,
    description: Option[String],
    state: String,
    iid: Int,
    assignees: Seq[PlaneUser],
    assignee: Option[PlaneUser],
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
    closed_by: Option[PlaneUser],
    // subscribed: Option[Boolean],
    // user_notes_count: Int,
    due_date: Option[Date],
    web_url: String,
    references: Option[PlaneReference],
    time_stats: Option[PlaneTimeslot],
    confidential: Option[Boolean],
    // discussion_locked: Option[Boolean],
    _links: Option[PlaneLinks],
    task_completion_status: Option[PlaneTaskCompletionStatus]
)

case class PlaneIssuesSearchResult(
    issues: Seq[PlaneIssue],
    totalNumberOfItems: Option[Int],
    totalPages: Option[Int],
    perPage: Option[Int],
    page: Option[Int],
    nextPage: Option[Int],
    prevPage: Option[Int]
)

object PlaneMilestone {
  import models.BaseFormat._
  implicit val jsonFormat: Format[PlaneMilestone] =
    Json.format[PlaneMilestone]
}
object PlaneUser {
  implicit val jsonFormat: Format[PlaneUser] = Json.format[PlaneUser]
}
object PlaneReference {
  implicit val jsonFormat: Format[PlaneReference] =
    Json.format[PlaneReference]
}
object PlaneTimeslot {
  implicit val jsonFormat: Format[PlaneTimeslot] = Json.format[PlaneTimeslot]
}
object PlaneLinks {
  implicit val jsonFormat: Format[PlaneLinks] = Json.format[PlaneLinks]
}
object PlaneTaskCompletionStatus {
  implicit val jsonFormat: Format[PlaneTaskCompletionStatus] =
    Json.format[PlaneTaskCompletionStatus]
}
object PlaneIssue {
  import models.BaseFormat._
  implicit val jsonFormat: Format[PlaneIssue] = Json.format[PlaneIssue]
}
object PlaneIssuesSearchResult {
  implicit val jsonFormat: Format[PlaneIssuesSearchResult] =
    Json.format[PlaneIssuesSearchResult]
}
