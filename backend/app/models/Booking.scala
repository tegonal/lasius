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

import models.LocalDateTimeWithTimeZone.DateTimeHelper
import models.ProjectId.ProjectReference
import org.joda.time.DateTime
import play.api.libs.json._
import models.BaseFormat._

import scala.annotation.nowarn

@SerialVersionUID(1241414)
case class BookingStub(bookingType: BookingType,
                       projectReference: Option[ProjectReference],
                       tags: Set[Tag],
                       bookingHash: Long)

object BookingStub {
  def apply(bookingType: BookingType,
            projectReference: Option[ProjectReference],
            tags: Set[Tag]): BookingStub =
    BookingStub(bookingType,
                projectReference,
                tags,
                BookingHash.createHash(projectReference, tags))

  def apply(projectReference: ProjectReference, tags: Set[Tag]): BookingStub =
    BookingStub(ProjectBooking,
                Some(projectReference),
                tags,
                BookingHash.createHash(Some(projectReference), tags))

  implicit val bookingStubFormat: Format[BookingStub] =
    Json.using[Json.WithDefaultValues].format[BookingStub]
}

@SerialVersionUID(1241414)
@deprecated("Don't use events based on Booking V1", "LasiusV.1")
case class Booking(id: BookingId,
                   start: DateTime,
                   end: Option[DateTime],
                   userId: String,
                   categoryId: String,
                   projectId: String,
                   tags: Set[Tag],
                   comment: Option[String] = None)
    extends BaseEntity[BookingId] {

  /** Migration to V2, reflect category as tag and append it additionally to
    * projectId to ensure project is still unique, drop comment
    *
    * @return
    */
  def toV2(users: Seq[User], projects: Seq[Project]): BookingV2 = {
    val projectKey = projectId + "@" + categoryId
    val user = users
      .find(_.key == userId)
      .getOrElse(sys.error(
        s"Cannot migrate current booking $this, user with key $userId not found"))
    val project = projects
      .find(_.key == projectKey)
      .getOrElse(sys.error(
        s"Cannot migrate current booking $this, project with key $projectKey not found"))
    val projectReference = project.reference
    val allTags          = tags + SimpleTag(TagId(categoryId))
    BookingV2(
      id,
      start.toLocalDateTimeWithZone,
      end.map(_.toLocalDateTimeWithZone),
      user.reference,
      project.organisationReference,
      projectReference,
      allTags,
      BookingHash.createHash(Some(projectReference), allTags)
    )
  }
}

object Booking {
  @nowarn("cat=deprecation")
  implicit val bookingFormat: Format[Booking] =
    Json.using[Json.WithDefaultValues].format[Booking]
}
