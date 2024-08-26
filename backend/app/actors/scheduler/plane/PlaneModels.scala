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
import models.BaseFormat._

import java.util.Date

case class PlaneIssue(
    id: String,
    created_at: DateTime,
    updated_at: DateTime,
    estimate_point: Option[Int],
    name: String,
    description_html: Option[String],
    description_stripped: Option[String],
    priority: Option[String],
    start_date: Option[Date],
    target_date: Option[Date],
    sequence_id: Int,
    sort_order: Double,
    completed_at: Option[DateTime],
    archived_at: Option[DateTime],
    // is_draft: Boolean, // currently not available on Plane UI
    created_by: String,
    updated_by: Option[String],
    project: String,
    workspace: String,
    parent: Option[String],
    state: Option[String],
    assignees: Seq[String],
    labels: Seq[String]
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

object PlaneIssue {
  import models.BaseFormat._
  implicit val jsonFormat: Format[PlaneIssue] = Json.format[PlaneIssue]
}
object PlaneIssuesSearchResult {
  import models.BaseFormat._
  implicit val jsonFormat: Format[PlaneIssuesSearchResult] =
    Json.format[PlaneIssuesSearchResult]
}
