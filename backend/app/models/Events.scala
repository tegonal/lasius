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
import models.BaseFormat._
import models.LocalDateTimeWithTimeZone.DateTimeHelper
import models.OrganisationId.OrganisationReference
import models.ProjectId.ProjectReference
import models.UserId.UserReference
import org.joda.time.{DateTime, Duration, LocalDate}
import play.api.libs.json._
import play.api.mvc.WebSocket.MessageFlowTransformer

import scala.annotation.nowarn

sealed trait PersistedEvent

case object UndefinedEvent extends PersistedEvent

/*
 * UserLoggedIn events
 */
@deprecated("Don't use events based on UserId", "LasiusV1.1")
case class UserLoggedIn(userId: String) extends PersistedEvent {
  def toV2(users: Seq[User]): UserLoggedInV2 = UserLoggedInV2(
    users
      .find(_.key == userId)
      .getOrElse(sys.error(
        s"Cannot migrate Event $this, User with key $userId not found"))
      .reference
  )
}

case class UserLoggedInV2(userReference: UserReference) extends PersistedEvent

/*
 * UserTimeBookingInitialized events
 */
@deprecated("Don't use events based on UserId", "LasiusV1.1")
case class UserTimeBookingInitialized(userId: String) extends PersistedEvent {
  def toV2(users: Seq[User]): UserTimeBookingInitializedV2 =
    UserTimeBookingInitializedV2(
      users
        .find(_.key == userId)
        .getOrElse(sys.error(
          s"Cannot migrate Event $this, User with key $userId not found"))
        .reference)
}

case class UserTimeBookingInitializedV2(userReference: UserReference)
    extends PersistedEvent

/*
 * UserTimeBookingPaused events
 */
@deprecated("Don't use Pause", "LasiusV1")
case class UserTimeBookingPaused(bookingId: BookingId, time: DateTime)
    extends PersistedEvent

/*
 * UserTimeBookingStarted events
 */
@deprecated("Don't use events based on Booking V1", "LasiusV1.1")
case class UserTimeBookingStarted(booking: Booking) extends PersistedEvent {
  def toV2(users: Seq[User], projects: Seq[Project]): UserTimeBookingStartedV2 =
    UserTimeBookingStartedV2(booking.toV2(users, projects))
}

@deprecated("Use UserTimeBookingStartedV3", "LasiusV1.2")
case class UserTimeBookingStartedV2(booking: BookingV2) extends PersistedEvent {
  def toV3: UserTimeBookingStartedV3 =
    UserTimeBookingStartedV3(
      booking.id,
      bookingType = ProjectBooking,
      start = booking.start,
      userReference = booking.userReference,
      organisationReference = booking.organisationReference,
      projectReference = Some(booking.projectReference),
      tags = booking.tags,
      bookingHash = booking.bookingHash
    )
}
case class UserTimeBookingStartedV3(
    id: BookingId,
    bookingType: BookingType,
    start: LocalDateTimeWithTimeZone,
    userReference: UserReference,
    organisationReference: OrganisationReference,
    projectReference: Option[ProjectReference],
    tags: Set[Tag],
    bookingHash: Long)
    extends PersistedEvent {
  def toBooking(
      endOrDuration: Either[LocalDateTimeWithTimeZone, Duration]): BookingV3 =
    BookingV3(
      id = id,
      bookingType = bookingType,
      start = start,
      end = endOrDuration.left.toOption,
      duration = endOrDuration.fold(
        end => new Duration(start.toDateTime, end.toDateTime),
        identity),
      userReference = userReference,
      organisationReference = organisationReference,
      projectReference = projectReference,
      tags = tags,
      bookingHash = bookingHash
    )

  val stub: BookingStub =
    BookingStub(bookingType, projectReference, tags, bookingHash)
}

object UserTimeBookingStartedV3 {
  def apply(id: BookingId,
            bookingType: BookingType,
            start: LocalDateTimeWithTimeZone,
            userReference: UserReference,
            organisationReference: OrganisationReference,
            projectReference: Option[ProjectReference],
            tags: Set[Tag]): UserTimeBookingStartedV3 =
    UserTimeBookingStartedV3(
      id = id,
      bookingType = bookingType,
      start = start,
      userReference = userReference,
      organisationReference = organisationReference,
      projectReference = projectReference,
      tags = tags,
      bookingHash = BookingHash.createHash(projectReference, tags)
    )

  def apply(id: BookingId,
            start: LocalDateTimeWithTimeZone,
            userReference: UserReference,
            organisationReference: OrganisationReference,
            projectReference: ProjectReference,
            tags: Set[Tag]): UserTimeBookingStartedV3 =
    UserTimeBookingStartedV3(
      id = id,
      bookingType = ProjectBooking,
      start = start,
      userReference = userReference,
      organisationReference = organisationReference,
      projectReference = Some(projectReference),
      tags = tags,
      bookingHash = BookingHash.createHash(Some(projectReference), tags)
    )

  implicit val format: Format[UserTimeBookingStartedV3] =
    Json.format[UserTimeBookingStartedV3]
}

/*
 * UserTimeBookingStopped events
 */

@deprecated("Don't use events based on Booking V1", "LasiusV1.1")
case class UserTimeBookingStopped(booking: Booking) extends PersistedEvent {
  def toV2(users: Seq[User], projects: Seq[Project]): UserTimeBookingStoppedV2 =
    UserTimeBookingStoppedV2(booking.toV2(users, projects))
}

@deprecated("Use UserTimeBookingStoppedV3", "LasiusV1.2")
case class UserTimeBookingStoppedV2(booking: BookingV2) extends PersistedEvent {
  def toV3: UserTimeBookingStoppedV3 =
    UserTimeBookingStoppedV3(booking = booking.toV3)
}
case class UserTimeBookingStoppedV3(booking: BookingV3) extends PersistedEvent

/*
 * UserTimeBookingEdited events
 */
@deprecated("Use UserTimeBookingEditedV4", "LasiusV1")
case class UserTimeBookingEdited(booking: Booking,
                                 start: DateTime,
                                 end: DateTime)
    extends PersistedEvent {
  @nowarn("cat=deprecation")
  def toV2: UserTimeBookingEditedV2 =
    UserTimeBookingEditedV2(booking,
                            booking.copy(start = start, end = Some(end)))
}

@deprecated("Use UserTimeBookingEditedV4", "LasiusV1.1")
case class UserTimeBookingEditedV2(booking: Booking, updatedBooking: Booking)
    extends PersistedEvent {
  def toV3(users: Seq[User], projects: Seq[Project]): UserTimeBookingEditedV3 =
    UserTimeBookingEditedV3(booking.toV2(users, projects),
                            updatedBooking.toV2(users, projects))
}

@deprecated("Use UserTimeBookingEditedV4", "LasiusV1.2")
case class UserTimeBookingEditedV3(booking: BookingV2,
                                   updatedBooking: BookingV2)
    extends PersistedEvent {
  def toV4: UserTimeBookingEditedV4 =
    UserTimeBookingEditedV4(booking.toV3, updatedBooking.toV3)
}

case class UserTimeBookingEditedV4(booking: BookingV3,
                                   updatedBooking: BookingV3)
    extends PersistedEvent

case class UserTimeBookingInProgressEdited(
    booking: UserTimeBookingStartedV3,
    updatedBooking: UserTimeBookingStartedV3)
    extends PersistedEvent

/*
 * UserTimeBookingRemoved events
 */
@deprecated("Use UserTimeBookingRemovedV3", "LasiusV1.1")
case class UserTimeBookingRemoved(booking: Booking) extends PersistedEvent {
  def toV2(users: Seq[User], projects: Seq[Project]): UserTimeBookingRemovedV2 =
    UserTimeBookingRemovedV2(booking.toV2(users, projects))
}
@deprecated("Use UserTimeBookingRemovedV3", "LasiusV1.2")
case class UserTimeBookingRemovedV2(booking: BookingV2) extends PersistedEvent {
  def toV3: UserTimeBookingRemovedV3 =
    UserTimeBookingRemovedV3(booking.toV3)
}
case class UserTimeBookingRemovedV3(booking: BookingV3) extends PersistedEvent

/*
 * UserTimeBookingAdded events
 */
@deprecated("Use UserTimeBookingAddedV3", "LasiusV1.1")
case class UserTimeBookingAdded(booking: Booking) extends PersistedEvent {
  def toV2(users: Seq[User], projects: Seq[Project]): UserTimeBookingAddedV2 =
    UserTimeBookingAddedV2(booking.toV2(users, projects))
}
@deprecated("Use UserTimeBookingAddedV3", "LasiusV1.2")
case class UserTimeBookingAddedV2(booking: BookingV2) extends PersistedEvent {
  def toV3: UserTimeBookingAddedV3 = UserTimeBookingAddedV3(
    id = booking.id,
    bookingType = ProjectBooking,
    userReference = booking.userReference,
    organisationReference = booking.organisationReference,
    projectReference = Some(booking.projectReference),
    tags = booking.tags,
    start = booking.start.toDateTime,
    endOrDuration = Left(booking.end.get.toDateTime)
  )
}

case class UserTimeBookingAddedV3(
    id: BookingId,
    bookingType: BookingType,
    userReference: UserReference,
    organisationReference: OrganisationReference,
    projectReference: Option[ProjectReference],
    tags: Set[Tag],
    start: DateTime,
    endOrDuration: Either[DateTime, org.joda.time.Duration]
) extends PersistedEvent {
  def toBooking: BookingV3 = BookingV3(
    id = id,
    bookingType = bookingType,
    start = start.toLocalDateTimeWithZone,
    end = endOrDuration.left.toOption.map(_.toLocalDateTimeWithZone),
    duration =
      endOrDuration.fold(new org.joda.time.Duration(start, _), identity),
    userReference = userReference,
    organisationReference = organisationReference,
    projectReference = projectReference,
    tags = tags
  )
}

object UserTimeBookingAddedV3 {
  def apply(booking: BookingV3): UserTimeBookingAddedV3 =
    UserTimeBookingAddedV3(
      id = booking.id,
      bookingType = booking.bookingType,
      userReference = booking.userReference,
      organisationReference = booking.organisationReference,
      projectReference = booking.projectReference,
      tags = booking.tags,
      start = booking.start.toDateTime,
      endOrDuration = booking.end.map(_.toDateTime).toLeft(booking.duration)
    )
}

/*
 * UserTimeBookingStartTimeChanged events
 */
case class UserTimeBookingStartTimeChanged(bookingId: BookingId,
                                           fromStart: DateTime,
                                           toStart: DateTime)
    extends PersistedEvent

sealed trait InEvent

case class HelloServer(client: String) extends InEvent

case object Ping extends InEvent

case object Pong extends OutEvent

sealed trait OutEvent

case object HelloClient extends OutEvent

@deprecated("Don't use events based on UserId", "LasiusV1.1")
case class UserLoggedOut(userId: String) extends OutEvent with PersistedEvent {
  def toV2(users: Seq[User]): UserLoggedOutV2 =
    UserLoggedOutV2(
      users
        .find(_.key == userId)
        .getOrElse(sys.error(
          s"Cannot migrate Event $this, User with key $userId not found"))
        .reference)
}

case class UserLoggedOutV2(userReference: UserReference)
    extends OutEvent
    with PersistedEvent

case class CurrentUserTimeBooking(userReference: UserReference,
                                  day: LocalDate,
                                  booking: Option[UserTimeBookingStartedV3],
                                  totalBySameBookingInMillis: Option[Duration],
                                  totalByDayInMillis: Duration)

object CurrentUserTimeBooking {
  implicit val format: Format[CurrentUserTimeBooking] =
    Json.format[CurrentUserTimeBooking]
}

case class CurrentUserTimeBookingEvent(booking: CurrentUserTimeBooking)
    extends OutEvent

case class CurrentOrganisationTimeBookings(
    orgId: OrganisationId,
    timeBookings: Seq[CurrentUserTimeBooking])
    extends OutEvent

object CurrentOrganisationTimeBookings {
  implicit val format: Format[CurrentOrganisationTimeBookings] =
    Json.format[CurrentOrganisationTimeBookings]
}

case class UserTimeBookingHistoryEntryCleaned(userId: UserId) extends OutEvent

case class UserTimeBookingHistoryEntryAdded(booking: BookingV3) extends OutEvent

case class UserTimeBookingHistoryEntryRemoved(bookingId: BookingId)
    extends OutEvent

case class UserTimeBookingHistoryEntryChanged(booking: BookingV3)
    extends OutEvent

case class UserTimeBookingInProgressEntryChanged(
    booking: Option[UserTimeBookingStartedV3])
    extends OutEvent

case class UserTimeBookingByProjectEntryCleaned(userId: UserId) extends OutEvent

case class UserTimeBookingByTagEntryCleaned(userId: UserId) extends OutEvent

case class UserTimeBookingByProjectEntryAdded(booking: BookingByProject)
    extends OutEvent

case class UserTimeBookingByTagEntryAdded(booking: BookingByTag)
    extends OutEvent

case class UserTimeBookingByTypeEntryAdded(booking: BookingByType)
    extends OutEvent

case class UserTimeBookingByProjectEntryRemoved(booking: BookingByProject)
    extends OutEvent

case class UserTimeBookingByTagEntryRemoved(booking: BookingByTag)
    extends OutEvent

case class UserTimeBookingByTypeEntryRemoved(booking: BookingByType)
    extends OutEvent

case class FavoriteAdded(userId: UserId,
                         orgId: OrganisationId,
                         bookingStub: BookingStub)
    extends OutEvent

case class FavoriteRemoved(userId: UserId,
                           orgId: OrganisationId,
                           bookingStub: BookingStub)
    extends OutEvent

case class TagCacheChanged(projectId: ProjectId,
                           removed: Set[Tag],
                           added: Set[Tag])
    extends OutEvent

object InEvent {
  implicit val inEventFormat: OFormat[InEvent] =
    derived.flat.oformat[InEvent](defaultTypeFormat)
}

object OutEvent {
  implicit val currentUserTimeBookingFormat: Format[CurrentUserTimeBooking] =
    Json.format[CurrentUserTimeBooking]
  @nowarn("cat=deprecation")
  implicit val outEventFormat: OFormat[OutEvent] =
    derived.flat.oformat[OutEvent](defaultTypeFormat)
}

object Events {
  implicit val messageFlowTransformer
      : MessageFlowTransformer[InEvent, OutEvent] =
    MessageFlowTransformer.jsonMessageFlowTransformer[InEvent, OutEvent]
}

object PersistedEvent {

  implicit val eventFormat: OFormat[PersistedEvent] =
    derived.flat.oformat[PersistedEvent](BaseFormat.defaultTypeFormat)
}
