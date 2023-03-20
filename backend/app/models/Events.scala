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
import models.UserId.UserReference
import org.joda.time.{DateTime, Duration, LocalDate}
import play.api.libs.json._
import play.api.mvc.WebSocket.MessageFlowTransformer

import java.util.UUID
import scala.annotation.nowarn

sealed trait PersistedEvent

case object UndefinedEvent extends PersistedEvent

@deprecated("Don't use events based on UserId", "LasiusV1.1")
case class UserLoggedIn(userId: String) extends PersistedEvent {
  def toV2(users: Seq[User]): UserLoggedInV2 = UserLoggedInV2(
    users
      .find(_.key == userId)
      .getOrElse(sys.error(
        s"Cannot migrate Event $this, User with key $userId not found"))
      .getReference()
  )
}

case class UserLoggedInV2(userReference: UserReference) extends PersistedEvent

@deprecated("Don't use events based on UserId", "LasiusV1.1")
case class UserTimeBookingInitialized(userId: String) extends PersistedEvent {
  def toV2(users: Seq[User]): UserTimeBookingInitializedV2 =
    UserTimeBookingInitializedV2(
      users
        .find(_.key == userId)
        .getOrElse(sys.error(
          s"Cannot migrate Event $this, User with key $userId not found"))
        .getReference())
}

case class UserTimeBookingInitializedV2(userReference: UserReference)
    extends PersistedEvent

@deprecated("Don't use Pause", "LasiusV1")
case class UserTimeBookingPaused(bookingId: BookingId, time: DateTime)
    extends PersistedEvent

/*
 * BookingV1 events
 */
@deprecated("Don't use events based on Booking V1", "LasiusV1.1")
case class UserTimeBookingStarted(booking: Booking) extends PersistedEvent {
  def toV2(users: Seq[User], projects: Seq[Project]): UserTimeBookingStartedV2 =
    UserTimeBookingStartedV2(booking.toV2(users, projects))
}

@deprecated("Don't use events based on Booking V1", "LasiusV1.1")
case class UserTimeBookingStopped(booking: Booking) extends PersistedEvent {
  def toV2(users: Seq[User], projects: Seq[Project]): UserTimeBookingStoppedV2 =
    UserTimeBookingStoppedV2(booking.toV2(users, projects))
}

@deprecated("Don't use events based on Booking V1", "LasiusV1.1")
case class UserTimeBookingRemoved(booking: Booking) extends PersistedEvent {
  def toV2(users: Seq[User], projects: Seq[Project]): UserTimeBookingRemovedV2 =
    UserTimeBookingRemovedV2(booking.toV2(users, projects))
}

@deprecated("Use UserTimeBookingAddedV2", "LasiusV1.1")
case class UserTimeBookingAdded(booking: Booking) extends PersistedEvent {
  def toV2(users: Seq[User], projects: Seq[Project]): UserTimeBookingAddedV2 =
    UserTimeBookingAddedV2(booking.toV2(users, projects))
}

@deprecated("Use UserTimeBookingEditedV3", "LasiusV1")
case class UserTimeBookingEdited(booking: Booking,
                                 start: DateTime,
                                 end: DateTime)
    extends PersistedEvent {
  @nowarn("cat=deprecation")
  def toV2(): UserTimeBookingEditedV2 =
    UserTimeBookingEditedV2(booking,
                            booking.copy(start = start, end = Some(end)))
}

@deprecated("Use UserTimeBookingEditedV3", "LasiusV1.1")
case class UserTimeBookingEditedV2(booking: Booking, updatedBooking: Booking)
    extends PersistedEvent {
  def toV3(users: Seq[User], projects: Seq[Project]): UserTimeBookingEditedV3 =
    UserTimeBookingEditedV3(booking.toV2(users, projects),
                            updatedBooking.toV2(users, projects))
}

/*
 * BookingV2 events
 */
case class UserTimeBookingStartedV2(booking: BookingV2) extends PersistedEvent

case class UserTimeBookingStoppedV2(booking: BookingV2) extends PersistedEvent

case class UserTimeBookingRemovedV2(booking: BookingV2) extends PersistedEvent

case class UserTimeBookingAddedV2(booking: BookingV2) extends PersistedEvent

case class UserTimeBookingEditedV3(booking: BookingV2,
                                   updatedBooking: BookingV2)
    extends PersistedEvent

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
        .getReference())
}

case class UserLoggedOutV2(userReference: UserReference)
    extends OutEvent
    with PersistedEvent

case class CurrentUserTimeBooking(userReference: UserReference,
                                  day: LocalDate,
                                  booking: Option[BookingV2],
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

case class UserTimeBookingHistoryEntryAdded(booking: BookingV2) extends OutEvent

case class UserTimeBookingHistoryEntryRemoved(bookingId: BookingId)
    extends OutEvent

case class UserTimeBookingHistoryEntryChanged(booking: BookingV2)
    extends OutEvent

case class UserTimeBookingByProjectEntryCleaned(userId: UserId) extends OutEvent

case class UserTimeBookingByCategoryEntryCleaned(userId: UserId)
    extends OutEvent

case class UserTimeBookingByTagEntryCleaned(userId: UserId) extends OutEvent

case class UserTimeBookingByProjectEntryAdded(booking: BookingByProject)
    extends OutEvent

case class UserTimeBookingByTagEntryAdded(booking: BookingByTag)
    extends OutEvent

case class UserTimeBookingByProjectEntryRemoved(booking: BookingByProject)
    extends OutEvent

case class UserTimeBookingByTagEntryRemoved(booking: BookingByTag)
    extends OutEvent

case class FavoriteAdded(userId: UserId,
                         orgId: OrganisationId,
                         bookingStub: BookingStub)
    extends OutEvent

case class FavoriteRemoved(userId: UserId,
                           orgId: OrganisationId,
                           bookingStub: BookingStub)
    extends OutEvent

case class LatestTimeBooking(userId: UserId, history: Seq[BookingStub])
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

  val x = Json.toJson(HelloClient: OutEvent)
}

object Events {
  implicit val messageFlowTransformer
      : MessageFlowTransformer[InEvent, OutEvent] =
    MessageFlowTransformer.jsonMessageFlowTransformer[InEvent, OutEvent]
}

object PersistedEvent {
  @nowarn("cat=deprecation")
  implicit val eventFormat: OFormat[PersistedEvent] =
    derived.flat.oformat[PersistedEvent](BaseFormat.defaultTypeFormat)
}
