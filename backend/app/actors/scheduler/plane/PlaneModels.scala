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

import play.api.libs.json._

import java.util.Date

case class PlaneIssueWrapper(
    grouped_by: Option[String],
    sub_grouped_by: Option[String],
    total_count: Int,
    next_cursor: String,
    prev_cursor: String,
    next_page_results: Boolean,
    prev_page_results: Boolean,
    count: Int,
    total_pages: Int,
    total_results: Int,
    extra_stats: Option[String],
    results: Seq[PlaneIssue],
)

case class PlaneLabel(
    id: String,
    // created_at: DateTime, // Nannosecond format not parsable by joda time "2024-08-27T14:33:01.364694+02:00"
    // updated_at: DateTime,
    name: String,
    description: String,
    color: String,
    sort_order: Double,
    created_by: String,
    updated_by: String,
    project: String,
    workspace: String,
    parent: Option[String]
)

case class PlaneState(
    id: String,
    name: String,
    color: String,
    group: String
)

case class PlaneProject(
    id: String,
    identifier: String,
    name: String,
    cover_image: String,
// icon_prop: IconProp,
    emoji: Option[String],
    description: String
)

case class PlaneIssue(
    id: String,
    // created_at: DateTime,
    // updated_at: DateTime,
    estimate_point: Option[Int],
    name: String,
    description_html: Option[String],
    description_stripped: Option[String],
    priority: Option[String],
    start_date: Option[Date],
    target_date: Option[Date],
    sequence_id: Int,
    sort_order: Double,
    // completed_at: Option[DateTime],
    // archived_at: Option[DateTime],
    // is_draft: Boolean, // currently not available on Plane UI
    created_by: String,
    updated_by: Option[String],
    project: PlaneProject,
    workspace: String,
    parent: Option[String],
    state: Option[PlaneState],
    assignees: Seq[String],
    labels: Option[Seq[PlaneLabel]],
)

case class PlaneIssuesSearchResult(
    issues: Seq[PlaneIssue],
    totalNumberOfItems: Option[Int],
    totalPages: Option[Int],
    perPage: Option[Int],
    page: Option[Int],
    nextPage: Option[Boolean],
    prevPage: Option[Boolean]
)

object PlaneIssueWrapper {
  implicit val jsonFormat: Format[PlaneIssueWrapper] =
    Json.format[PlaneIssueWrapper]
}

object PlaneLabel {
  implicit val jsonFormat: Format[PlaneLabel] = Json.format[PlaneLabel]
}

object PlaneState {
  implicit val jsonFormat: Format[PlaneState] = Json.format[PlaneState]
}

object PlaneProject {
  implicit val jsonFormat: Format[PlaneProject] = Json.format[PlaneProject]
}

object PlaneIssue {
  implicit val jsonFormat: Format[PlaneIssue] = Json.format[PlaneIssue]
}

object PlaneIssuesSearchResult {
  implicit val jsonFormat: Format[PlaneIssuesSearchResult] =
    Json.format[PlaneIssuesSearchResult]
}
