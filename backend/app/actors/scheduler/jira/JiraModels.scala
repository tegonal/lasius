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

import java.net.URI
import java.util.Date

import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import models.BaseFormat._

case class JiraAvatarUrls(`16x16`: String,
                          `24x24`: String,
                          `32x32`: String,
                          `48x48`: String)
case class JiraProject(self: URI,
                       id: String,
                       key: String,
                       name: String,
                       avatarUrls: Option[JiraAvatarUrls])
case class JiraIssueType(self: URI,
                         id: String,
                         description: String,
                         iconUrl: String,
                         name: String,
                         subtask: Boolean)
case class JiraVotes(self: URI, votes: Number, hasVoted: Boolean)
case class JiraFixVersion(self: URI,
                          id: String,
                          description: String,
                          name: String,
                          archived: Boolean,
                          released: Boolean)
case class JiraPerson(self: URI,
                      name: String,
                      emailAddress: String,
                      avatarUrls: JiraAvatarUrls,
                      displayName: String,
                      active: Boolean)
case class JiraProgress(progress: Number, total: Number)
case class JiraPriority(self: URI, iconUrl: String, name: String, id: String)
case class JiraLinkType(id: String,
                        name: String,
                        inward: String,
                        outward: String,
                        self: URI)
case class JiraWatches(self: URI, watchCount: Number, isWatching: Boolean)
case class JiraVersion(self: URI,
                       id: String,
                       description: String,
                       name: String,
                       archived: Boolean,
                       released: Boolean)
case class JiraStatusCategory(self: URI,
                              id: Number,
                              key: String,
                              colorName: String,
                              name: String)
case class JiraStatus(self: URI,
                      description: String,
                      iconUrl: String,
                      name: String,
                      id: String,
                      statusCategory: JiraStatusCategory)
case class JiraLabel(name: String)
case class JiraComponent(self: URI, id: Long, name: String, lead: JiraPerson)
case class JiraIssueResolution(description: String,
                               id: String,
                               name: String,
                               self: URI)
case class PrimaryJiraIssueFields(summary: Option[String],
                                  progress: Option[JiraProgress],
                                  issuetype: Option[JiraIssueType],
                                  votes: Option[JiraVotes],
                                  resolution: Option[JiraIssueResolution],
                                  resolutiondate: Option[String],
                                  timespent: Option[Int],
                                  creator: Option[JiraPerson],
                                  reporter: Option[JiraPerson],
                                  aggregatetimeoriginalestimate: Option[String],
                                  created: Option[Date],
                                  updated: Option[Date],
                                  description: Option[String],
                                  priority: Option[JiraPriority],
                                  duedate: Option[Date],
                                  watches: Option[JiraWatches],
                                  status: Option[JiraStatus],
                                  workratio: Option[Int],
                                  project: Option[JiraProject],
                                  aggregateprogress: Option[JiraProgress],
                                  lastViewed: Option[Date])

case class SecondaryJiraIssueFields(labels: Option[Seq[JiraLabel]],
                                    assignee: Option[JiraPerson],
                                    aggregatetimeestimate: Option[Number],
                                    versions: Option[Seq[JiraVersion]],
                                    fixVersions: Option[Seq[JiraVersion]],
                                    environment: Option[String],
                                    timeestimate: Option[Date],
                                    components: Option[Seq[JiraComponent]],
                                    aggregatetimespent: Option[Int])

case class SelfReferenceJiraIssueFields(issuelinks: Option[Seq[JiraIssueLink]],
                                        subtasks: Option[Seq[JiraIssue]])

case class JiraIssueFields(primary: PrimaryJiraIssueFields,
                           secondary: SecondaryJiraIssueFields,
                           ref: SelfReferenceJiraIssueFields)

case class JiraIssue(expand: Option[String] = None,
                     id: String,
                     self: URI,
                     key: String,
                     fields: Option[JiraIssueFields] = None)
case class JiraIssueLink(id: String,
                         self: URI,
                         `type`: Option[JiraLinkType],
                         inwardIssue: Option[JiraIssue])
case class JiraSearchResult(expand: Option[String] = None,
                            startAt: Int,
                            maxResults: Int,
                            total: Int,
                            issues: Seq[JiraIssue])

object JiraAvatarUrls {
  implicit val jsonFormat: Format[JiraAvatarUrls] = Json.format[JiraAvatarUrls]
}
object JiraProject {
  implicit val jsonFormat: Format[JiraProject] = Json.format[JiraProject]
}
object JiraIssueType {
  implicit val jsonFormat: Format[JiraIssueType] = Json.format[JiraIssueType]
}
object JiraVotes {
  implicit val jsonFormat: Format[JiraVotes] = Json.format[JiraVotes]
}
object JiraFixVersion {
  implicit val jsonFormat: Format[JiraFixVersion] = Json.format[JiraFixVersion]
}
object JiraPerson {
  implicit val jsonFormat: Format[JiraPerson] = Json.format[JiraPerson]
}
object JiraProgress {
  implicit val jsonFormat: Format[JiraProgress] = Json.format[JiraProgress]
}
object JiraPriority {
  implicit val jsonFormat: Format[JiraPriority] = Json.format[JiraPriority]
}
object JiraLinkType {
  implicit val jsonFormat: Format[JiraLinkType] = Json.format[JiraLinkType]
}
object JiraWatches {
  implicit val jsonFormat: Format[JiraWatches] = Json.format[JiraWatches]
}
object JiraIssueLink {
  implicit val jsonFormat: Format[JiraIssueLink] = Json.format[JiraIssueLink]
}
object JiraVersion {
  implicit val jsonFormat: Format[JiraVersion] = Json.format[JiraVersion]
}
object JiraStatusCategory {
  implicit val jsonFormat: Format[JiraStatusCategory] =
    Json.format[JiraStatusCategory]
}
object JiraStatus {
  implicit val jsonFormat: Format[JiraStatus] = Json.format[JiraStatus]
}
object JiraLabel {
  implicit val jsonFormat: Format[JiraLabel] = Json.format[JiraLabel]
}
object JiraComponent {
  implicit val jsonFormat: Format[JiraComponent] = Json.format[JiraComponent]
}
object JiraIssue {
  implicit val jsonFormat: Format[JiraIssue] = Json.format[JiraIssue]
}
object JiraIssueResolution {
  implicit val jsonFormat: Format[JiraIssueResolution] =
    Json.format[JiraIssueResolution]
}
object PrimaryJiraIssueFields {
  implicit val jsonFormat: Format[PrimaryJiraIssueFields] =
    Json.format[PrimaryJiraIssueFields]
}
object SecondaryJiraIssueFields {
  implicit val jsonFormat: Format[SecondaryJiraIssueFields] =
    Json.format[SecondaryJiraIssueFields]
}
object SelfReferenceJiraIssueFields {
  implicit val reads: Reads[SelfReferenceJiraIssueFields] =
    (JsPath \ "issuelinks")
      .lazyReadNullable[Seq[JiraIssueLink]](Reads.seq(JiraIssueLink.jsonFormat))
      .and((JsPath \ "subtasks")
        .lazyReadNullable[Seq[JiraIssue]](Reads.seq(JiraIssue.jsonFormat)))(
        SelfReferenceJiraIssueFields.apply _)

  implicit val writes: Writes[SelfReferenceJiraIssueFields] =
    (JsPath \ "issuelinks")
      .lazyWriteNullable[Seq[JiraIssueLink]](
        Writes.seq(JiraIssueLink.jsonFormat))
      .and((JsPath \ "subtasks")
        .lazyWriteNullable[Seq[JiraIssue]](Writes.seq(JiraIssue.jsonFormat)))(
        unlift(SelfReferenceJiraIssueFields.unapply))
}

object JiraIssueFields {
  implicit val issueFieldsReads: Reads[JiraIssueFields] =
    JsPath
      .read[PrimaryJiraIssueFields]
      .and(JsPath.read[SecondaryJiraIssueFields])
      .and(JsPath.read[SelfReferenceJiraIssueFields])(JiraIssueFields.apply _)

  implicit val issueFieldsWrites: Writes[JiraIssueFields] =
    JsPath
      .write[PrimaryJiraIssueFields]
      .and(JsPath.write[SecondaryJiraIssueFields])
      .and(JsPath.write[SelfReferenceJiraIssueFields])(
        unlift(JiraIssueFields.unapply))
}

object JiraSearchResult {
  implicit val jsonFormat: Format[JiraSearchResult] =
    Json.format[JiraSearchResult]
}
