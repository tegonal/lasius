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

package models

import julienrf.json.derived
import net.openhft.hashing.LongHashFunction
import play.api.libs.json._

import java.net.URI

sealed trait Tag {
  val id: TagId
  lazy val hash: Long = LongHashFunction.xx.hashChars(id.value)
}

sealed trait NamedTag extends Tag {
  val summary: Option[String]
}

/** Represents a tag with a combination of other related tags
  */
sealed trait GroupedTags extends Tag {
  val relatedTags: Seq[_ <: Tag]
  override lazy val hash: Long = LongHashFunction.xx.hashChars(
    (id.value +: relatedTags.map(_.id.value)).mkString(","))
}

case class TagGroup(id: TagId,
                    relatedTags: Seq[SimpleTag],
                    // type attribute only needed to generate correct swagger definition
                    `type`: String = classOf[TagGroup].getSimpleName)
    extends BaseEntity[TagId]
    with GroupedTags

case class SimpleTag(id: TagId,
                     // type attribute only needed to generate correct swagger definition
                     `type`: String = classOf[SimpleTag].getSimpleName)
    extends BaseEntity[TagId]
    with Tag

object SimpleTag {
  implicit val simpleTagFormat: Format[SimpleTag] = Json.format[SimpleTag]
}

object Tag {
  implicit val tagWrites =
    derived.flat.owrites[Tag](BaseFormat.defaultTypeFormat)
  val defaultTagReads = derived.flat.reads[Tag](BaseFormat.defaultTypeFormat)
  val tagReads = (JsPath \ "type").readNullable[String].flatMap {
    // by default map to "SimpleTag" to be compliant with old bookings based on TagId only
    case None =>
      JsPath().read[String].map[Tag](tag => SimpleTag(TagId(tag)))
    case Some(_) => defaultTagReads
  }
  implicit val tagFormat: Format[Tag] = Format(tagReads, tagWrites)
  implicit val setTagFormat: Format[Set[Tag]] =
    Format(Reads.set(tagReads), Writes.set(tagWrites))
  implicit val seqTagFormat: Format[Seq[Tag]] =
    Format(Reads.seq(tagReads), Writes.seq(tagWrites))
}

// gitlab tags
case class GitlabIssueTag(id: TagId,
                          projectId: Int,
                          summary: Option[String],
                          relatedTags: Seq[SimpleTag],
                          issueLink: String,
                          // type attribute only needed to generate correct swagger definition
                          `type`: String =
                            classOf[GitlabIssueTag].getSimpleName)
    extends GroupedTags
    with NamedTag

object GitlabIssueTag {
  implicit val issueTagFormat: Format[GitlabIssueTag] =
    Json.format[GitlabIssueTag]
}

// Jira tags
case class JiraIssueTag(id: TagId,
                        baseUrl: String,
                        summary: Option[String],
                        url: URI,
                        projectKey: String,
                        // type attribute only needed to generate correct swagger definition
                        `type`: String = classOf[JiraIssueTag].getSimpleName)
    extends NamedTag

object JiraIssueTag {
  implicit val issueTagFormat: Format[JiraIssueTag] = Json.format[JiraIssueTag]
}
