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

package domain.views

import actors.ClientReceiver
import akka.PersistentActorTestScope
import akka.actor.{ActorRef, Props}
import akka.pattern.StatusReply.Ack
import akka.testkit._
import core.{DBSession, SystemServices}
import domain.AggregateRoot.{InitializeViewLive, RestoreViewFromState}
import domain.UserTimeBookingAggregate.UserTimeBooking
import models.LocalDateTimeWithTimeZone.DateTimeHelper
import models._
import org.joda.time._
import org.specs2.matcher._
import org.specs2.mock._
import org.specs2.mock.mockito.MockitoMatchers
import util.MockAwaitable
import play.api.libs.json._
import play.api.test.PlaySpecification
import play.modules.reactivemongo.ReactiveMongoApi
import repositories._

class UserTimeBookingStatisticsViewSpec
    extends PlaySpecification
    with Mockito
    with MockAwaitable
    with MockitoMatchers
    with PersistentActorTestScope {
  sequential

  "UserTimeBookingStatisticsView UserTimeBookingInitialized" should {
    "delete collections" in new WithUserTimeBookingStatisticsViewMock {
      probe.send(
        actorRef,
        RestoreViewFromState(userReference,
                             0,
                             UserTimeBooking(userReference, None, Seq())))
      probe.expectMsg(RestoreViewFromStateSuccess)

      there.was(
        one(bookingByProjectRepository).deleteByUserReference(
          anyOf(userReference))(any[Format[BookingByProject]], any[DBSession]))
      there.was(
        one(bookingByTagRepository)
          .deleteByUserReference(anyOf(userReference))(
            any[Format[BookingByTag]],
            any[DBSession]))
    }
  }

  "UserTimeBookingStatisticsView UserTimeBookingStopped" should {
    "add new duration to stats" in {
      testAddDuration(booking => UserTimeBookingStoppedV3(booking))
    }

    "don't add stats if no enddate is specified" in {
      testAddDurationWithoutEnd(booking => UserTimeBookingStoppedV3(booking))
    }
  }

  "UserTimeBookingStatisticsView UserTimeBookingAdded" should {
    "add new duration to stats if end of booking is defined" in {
      testAddDuration(booking =>
        UserTimeBookingAddedV3(
          id = booking.id,
          bookingType = booking.bookingType,
          userReference = booking.userReference,
          organisationReference = booking.organisationReference,
          projectReference = booking.projectReference,
          tags = booking.tags,
          start = booking.start.toDateTime,
          endOrDuration = booking.end.map(_.toDateTime).toLeft(booking.duration)
        ))
    }

    "don't add stats if no enddate is specified" in {
      testAddDurationWithoutEnd(booking =>
        UserTimeBookingAddedV3(
          id = booking.id,
          bookingType = booking.bookingType,
          userReference = booking.userReference,
          organisationReference = booking.organisationReference,
          projectReference = booking.projectReference,
          tags = booking.tags,
          start = booking.start.toDateTime,
          endOrDuration = booking.end.map(_.toDateTime).toLeft(booking.duration)
        ))
    }

    "add duration over multiple days" in {
      testAddDurationOverSeveralDays(booking =>
        UserTimeBookingAddedV3(
          id = booking.id,
          bookingType = booking.bookingType,
          userReference = booking.userReference,
          organisationReference = booking.organisationReference,
          projectReference = booking.projectReference,
          tags = booking.tags,
          start = booking.start.toDateTime,
          endOrDuration = booking.end.map(_.toDateTime).toLeft(booking.duration)
        ))
    }
  }

  "UserTimeBookingStatisticsView UserTimeBookingRemoved" should {
    "remove duration from total of same day" in {
      testRemoveDuration
    }

    "remove duration over multiple days" in {
      testRemoveDurationOverSeveralDays
    }
  }

  "UserTimeBookingStatisticsView UserTimeBookingEdited" should {
    "edit duration from total of same day" in {
      testEditDuration
    }

    "edit duration over multiple days" in {
      testEditDurationOverSeveralDays
    }

    "add tag" in {
      testEditDurationAddTag
    }

    "remove tag" in {
      testEditDurationRemoveTag
    }
  }

  "UserTimeBookingStatisticsView UserTimeBookingStartTimeChanged" should {
    "do nothing" in new WithUserTimeBookingStatisticsViewMock {
      val day: DateTime        = DateTime.parse("2000-01-01")
      val start: DateTime      = day.plusHours(5)
      val bookingId: BookingId = BookingId()
      val newStart: DateTime   = start.minusHours(3)

      probe.send(actorRef, InitializeViewLive(userReference, 0))
      probe.expectMsg(JournalReadingViewIsLive)

      probe.send(actorRef,
                 UserTimeBookingStartTimeChanged(bookingId, start, newStart))
      probe.expectMsg(Ack)

      there.was(noCallsTo(bookingByProjectRepository))
      there.was(noCallsTo(bookingByTagRepository))
      there.was(noCallsTo(bookingByTypeRepository))
    }
  }

  "UserTimeBookingStatisticsView various cases" should {
    "LAS-24" in new WithUserTimeBookingStatisticsViewMock {
      probe.send(actorRef, InitializeViewLive(userReference, 0))
      probe.expectMsg(JournalReadingViewIsLive)

      // days from 23. to 28.
      val day1: DateTime = DateTime.parse("2015-04-23")
      val day2: DateTime = day1.plusDays(1)
      val day3: DateTime = day2.plusDays(1)
      val day4: DateTime = day3.plusDays(1)
      val day5: DateTime = day4.plusDays(1)
      val day6: DateTime = day5.plusDays(1)

      val projectId1: EntityReference[ProjectId] =
        EntityReference(ProjectId(), "Proj1")
      val projectId2: EntityReference[ProjectId] =
        EntityReference(ProjectId(), "Proj2")
      val tag1: SimpleTag = SimpleTag(TagId("LAS-22"))
      val tag2: SimpleTag = SimpleTag(TagId("Testsystem"))

      // booking 1
      val start1: DateTime =
        day1.withHourOfDay(13).withMinuteOfHour(42)
      val end1: DateTime =
        day6.withHourOfDay(9).withMinuteOfHour(17)

      val booking1: BookingV3 = BookingV3(
        id = BookingId(),
        start = start1.toLocalDateTimeWithZone,
        end = Some(end1.toLocalDateTimeWithZone),
        duration = new Duration(start1, end1),
        userReference = userReference,
        organisationReference = orgReference,
        projectReference = projectId1,
        tags = Set(tag1)
      )

      // booking 2
      val start2: DateTime = end1
      val end2: DateTime   = start2.withHourOfDay(12).withMinuteOfHour(2)
      val booking2: BookingV3 = BookingV3(
        id = BookingId(),
        start = start2.toLocalDateTimeWithZone,
        end = Some(end2.toLocalDateTimeWithZone),
        duration = new Duration(start2, end2),
        userReference = userReference,
        organisationReference = orgReference,
        projectReference = projectId2,
        tags = Set(tag2)
      )

      // booking 3
      val start3: DateTime = start2.withHourOfDay(12).withMinuteOfHour(50)
      val end3: DateTime   = start2.withHourOfDay(15).withMinuteOfHour(30)
      val booking3: BookingV3 = BookingV3(
        id = BookingId(),
        start = start3.toLocalDateTimeWithZone,
        end = Some(end3.toLocalDateTimeWithZone),
        duration = new Duration(start3, end3),
        userReference = userReference,
        organisationReference = orgReference,
        projectReference = projectId2,
        tags = Set(tag2)
      )

      // booking 4
      val start4: DateTime = start2.withHourOfDay(16).withMinuteOfHour(28)
      val end4: DateTime   = start2.withHourOfDay(21).withMinuteOfHour(41)
      val booking4: BookingV3 = BookingV3(
        id = BookingId(),
        start = start4.toLocalDateTimeWithZone,
        end = Some(end4.toLocalDateTimeWithZone),
        duration = new Duration(start4, end4),
        userReference = userReference,
        organisationReference = orgReference,
        projectReference = projectId2,
        tags = Set(tag2)
      )

      probe.send(actorRef, UserTimeBookingAddedV3(booking1))
      probe.expectMsg(Ack)
      probe.send(actorRef, UserTimeBookingAddedV3(booking2))
      probe.expectMsg(Ack)
      probe.send(actorRef, UserTimeBookingAddedV3(booking3))
      probe.expectMsg(Ack)
      probe.send(actorRef, UserTimeBookingAddedV3(booking4))
      probe.expectMsg(Ack)
    }
  }

  private def testAddDurationOverSeveralDays(
      eventFactory: BookingV3 => PersistedEvent)
      : WithUserTimeBookingStatisticsViewMock = {
    testHandleDurationOverSeveralDays(eventFactory) {
      (bookingByProjectRepository,
       bookingByTagRepository,
       bookingByTypeRepository,
       projectReference,
       tagId1,
       tagId2,
       tagId3,
       userReference,
       orgReference,
       day1,
       day2,
       day3,
       duration1,
       duration2,
       duration3) =>
        there.was(
          3.times(bookingByProjectRepository)
            .add {
              beLike[BookingByProject] {
                case BookingByProject(_,
                                      `userReference`,
                                      `orgReference`,
                                      `day1`,
                                      `projectReference`,
                                      `duration1`) =>
                  ok
                case BookingByProject(_,
                                      `userReference`,
                                      `orgReference`,
                                      `day2`,
                                      `projectReference`,
                                      `duration2`) =>
                  ok
                case BookingByProject(_,
                                      `userReference`,
                                      `orgReference`,
                                      `day3`,
                                      `projectReference`,
                                      `duration3`) =>
                  ok
              }
            }(any[Writes[BookingByProjectId]], any[DBSession]))

        there.was(
          3.times(bookingByTypeRepository)
            .add {
              beLike[BookingByType] {
                case BookingByType(_,
                                   `userReference`,
                                   `orgReference`,
                                   `day1`,
                                   `ProjectBooking`,
                                   `duration1`) =>
                  ok
                case BookingByType(_,
                                   `userReference`,
                                   `orgReference`,
                                   `day2`,
                                   `ProjectBooking`,
                                   `duration2`) =>
                  ok
                case BookingByType(_,
                                   `userReference`,
                                   `orgReference`,
                                   `day3`,
                                   `ProjectBooking`,
                                   `duration3`) =>
                  ok
              }
            }(any[Writes[BookingByTypeId]], any[DBSession]))

        there.was(
          9.times(bookingByTagRepository)
            .add {
              beLike[BookingByTag] {
                case BookingByTag(_,
                                  `userReference`,
                                  `orgReference`,
                                  `day1`,
                                  `tagId1`,
                                  `duration1`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `orgReference`,
                                  `day1`,
                                  `tagId2`,
                                  `duration1`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `orgReference`,
                                  `day1`,
                                  `tagId3`,
                                  `duration1`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `orgReference`,
                                  `day2`,
                                  `tagId1`,
                                  `duration2`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `orgReference`,
                                  `day2`,
                                  `tagId2`,
                                  `duration2`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `orgReference`,
                                  `day2`,
                                  `tagId3`,
                                  `duration2`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `orgReference`,
                                  `day3`,
                                  `tagId1`,
                                  `duration3`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `orgReference`,
                                  `day3`,
                                  `tagId2`,
                                  `duration3`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `orgReference`,
                                  `day3`,
                                  `tagId3`,
                                  `duration3`) =>
                  ok
              }
            }(any[Writes[BookingByTagId]], any[DBSession]))
    }
  }

  private def testRemoveDurationOverSeveralDays
      : WithUserTimeBookingStatisticsViewMock = {
    testHandleDurationOverSeveralDays(booking =>
      UserTimeBookingRemovedV3(booking)) {
      (bookingByProjectRepository,
       bookingByTagRepository,
       bookingByTypeRepository,
       projectReference,
       tagId1,
       tagId2,
       tagId3,
       userReference,
       orgReference,
       day1,
       day2,
       day3,
       duration1,
       duration2,
       duration3) =>
        there.was(
          3.times(bookingByProjectRepository)
            .subtract {
              beLike[BookingByProject] {
                case BookingByProject(_,
                                      `userReference`,
                                      `orgReference`,
                                      `day1`,
                                      `projectReference`,
                                      `duration1`) =>
                  ok
                case BookingByProject(_,
                                      `userReference`,
                                      `orgReference`,
                                      `day2`,
                                      `projectReference`,
                                      `duration2`) =>
                  ok
                case BookingByProject(_,
                                      `userReference`,
                                      `orgReference`,
                                      `day3`,
                                      `projectReference`,
                                      `duration3`) =>
                  ok
              }
            }(any[Writes[BookingByProjectId]], any[DBSession]))

        there.was(
          3.times(bookingByTypeRepository)
            .subtract {
              beLike[BookingByType] {
                case BookingByType(_,
                                   `userReference`,
                                   `orgReference`,
                                   `day1`,
                                   `ProjectBooking`,
                                   `duration1`) =>
                  ok
                case BookingByType(_,
                                   `userReference`,
                                   `orgReference`,
                                   `day2`,
                                   `ProjectBooking`,
                                   `duration2`) =>
                  ok
                case BookingByType(_,
                                   `userReference`,
                                   `orgReference`,
                                   `day3`,
                                   `ProjectBooking`,
                                   `duration3`) =>
                  ok
              }
            }(any[Writes[BookingByTypeId]], any[DBSession]))

        there.was(
          9.times(bookingByTagRepository)
            .subtract {
              beLike[BookingByTag] {
                case BookingByTag(_,
                                  `userReference`,
                                  `orgReference`,
                                  `day1`,
                                  `tagId1`,
                                  `duration1`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `orgReference`,
                                  `day1`,
                                  `tagId2`,
                                  `duration1`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `orgReference`,
                                  `day1`,
                                  `tagId3`,
                                  `duration1`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `orgReference`,
                                  `day2`,
                                  `tagId1`,
                                  `duration2`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `orgReference`,
                                  `day2`,
                                  `tagId2`,
                                  `duration2`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `orgReference`,
                                  `day2`,
                                  `tagId3`,
                                  `duration2`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `orgReference`,
                                  `day3`,
                                  `tagId1`,
                                  `duration3`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `orgReference`,
                                  `day3`,
                                  `tagId2`,
                                  `duration3`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `orgReference`,
                                  `day3`,
                                  `tagId3`,
                                  `duration3`) =>
                  ok
              }
            }(any[Writes[BookingByTagId]], any[DBSession]))
    }
  }

  private def testEditDurationOverSeveralDays
      : WithUserTimeBookingStatisticsViewMock = {
    val day1              = LocalDateTime.parse("2000-01-01")
    val day4LocalDateTime = day1.plusDays(3)
    val day4              = day4LocalDateTime.toLocalDate
    val newStop           = day4LocalDateTime.plusHours(12).toDateTime
    val newStart          = day1.plusHours(1).toDateTime

    val newDuration1 = Duration.standardHours(24 - 1)
    val newDuration3 = Duration.standardHours(24)
    val newDuration4 = Duration.standardHours(12)

    testHandleDurationOverSeveralDays(booking =>
      UserTimeBookingEditedV4(
        booking,
        booking.copy(start = newStart.toLocalDateTimeWithZone,
                     end = Some(newStop.toLocalDateTimeWithZone)))) {
      (bookingByProjectRepository,
       bookingByTagRepository,
       bookingByTypeRepository,
       projectReference,
       tagId1,
       tagId2,
       tagId3,
       userReference,
       orgReference,
       day1,
       day2,
       day3,
       duration1,
       duration2,
       duration3) =>
        // remove old durations
        there.was(
          3.times(bookingByProjectRepository)
            .subtract {
              beLike[BookingByProject] {
                case BookingByProject(_,
                                      `userReference`,
                                      `orgReference`,
                                      `day1`,
                                      `projectReference`,
                                      `duration1`) =>
                  ok
                case BookingByProject(_,
                                      `userReference`,
                                      `orgReference`,
                                      `day2`,
                                      `projectReference`,
                                      `duration2`) =>
                  ok
                case BookingByProject(_,
                                      `userReference`,
                                      `orgReference`,
                                      `day3`,
                                      `projectReference`,
                                      `duration3`) =>
                  ok
              }
            }(any[Writes[BookingByProjectId]], any[DBSession]))

        there.was(
          3.times(bookingByTypeRepository)
            .subtract {
              beLike[BookingByType] {
                case BookingByType(_,
                                   `userReference`,
                                   `orgReference`,
                                   `day1`,
                                   `ProjectBooking`,
                                   `duration1`) =>
                  ok
                case BookingByType(_,
                                   `userReference`,
                                   `orgReference`,
                                   `day2`,
                                   `ProjectBooking`,
                                   `duration2`) =>
                  ok
                case BookingByType(_,
                                   `userReference`,
                                   `orgReference`,
                                   `day3`,
                                   `ProjectBooking`,
                                   `duration3`) =>
                  ok
              }
            }(any[Writes[BookingByTypeId]], any[DBSession]))

        there.was(
          9.times(bookingByTagRepository)
            .subtract {
              beLike[BookingByTag] {
                case BookingByTag(_,
                                  `userReference`,
                                  `orgReference`,
                                  `day1`,
                                  `tagId1`,
                                  `duration1`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `orgReference`,
                                  `day1`,
                                  `tagId2`,
                                  `duration1`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `orgReference`,
                                  `day1`,
                                  `tagId3`,
                                  `duration1`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `orgReference`,
                                  `day2`,
                                  `tagId1`,
                                  `duration2`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `orgReference`,
                                  `day2`,
                                  `tagId2`,
                                  `duration2`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `orgReference`,
                                  `day2`,
                                  `tagId3`,
                                  `duration2`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `orgReference`,
                                  `day3`,
                                  `tagId1`,
                                  `duration3`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `orgReference`,
                                  `day3`,
                                  `tagId2`,
                                  `duration3`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `orgReference`,
                                  `day3`,
                                  `tagId3`,
                                  `duration3`) =>
                  ok
              }
            }(any[Writes[BookingByTagId]], any[DBSession]))

        // add new durations
        there.was(
          4.times(bookingByProjectRepository)
            .add {
              beLike[BookingByProject] {
                case BookingByProject(_,
                                      `userReference`,
                                      `orgReference`,
                                      `day1`,
                                      `projectReference`,
                                      `newDuration1`) =>
                  ok
                case BookingByProject(_,
                                      `userReference`,
                                      `orgReference`,
                                      `day2`,
                                      `projectReference`,
                                      `duration2`) =>
                  ok
                case BookingByProject(_,
                                      `userReference`,
                                      `orgReference`,
                                      `day3`,
                                      `projectReference`,
                                      `newDuration3`) =>
                  ok
                case BookingByProject(_,
                                      `userReference`,
                                      `orgReference`,
                                      `day4`,
                                      `projectReference`,
                                      `newDuration4`) =>
                  ok
              }
            }(any[Writes[BookingByProjectId]], any[DBSession]))

        there.was(
          4.times(bookingByTypeRepository)
            .add {
              beLike[BookingByType] {
                case BookingByType(_,
                                   `userReference`,
                                   `orgReference`,
                                   `day1`,
                                   `ProjectBooking`,
                                   `newDuration1`) =>
                  ok
                case BookingByType(_,
                                   `userReference`,
                                   `orgReference`,
                                   `day2`,
                                   `ProjectBooking`,
                                   `duration2`) =>
                  ok
                case BookingByType(_,
                                   `userReference`,
                                   `orgReference`,
                                   `day3`,
                                   `ProjectBooking`,
                                   `newDuration3`) =>
                  ok
                case BookingByType(_,
                                   `userReference`,
                                   `orgReference`,
                                   `day4`,
                                   `ProjectBooking`,
                                   `newDuration4`) =>
                  ok
              }
            }(any[Writes[BookingByTypeId]], any[DBSession]))

        there.was(
          12.times(bookingByTagRepository)
            .add {
              beLike[BookingByTag] {
                case BookingByTag(_,
                                  `userReference`,
                                  `orgReference`,
                                  `day1`,
                                  `tagId1`,
                                  `newDuration1`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `orgReference`,
                                  `day1`,
                                  `tagId2`,
                                  `newDuration1`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `orgReference`,
                                  `day1`,
                                  `tagId3`,
                                  `newDuration1`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `orgReference`,
                                  `day2`,
                                  `tagId1`,
                                  `duration2`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `orgReference`,
                                  `day2`,
                                  `tagId2`,
                                  `duration2`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `orgReference`,
                                  `day2`,
                                  `tagId3`,
                                  `duration2`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `orgReference`,
                                  `day3`,
                                  `tagId1`,
                                  `newDuration3`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `orgReference`,
                                  `day3`,
                                  `tagId2`,
                                  `newDuration3`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `orgReference`,
                                  `day3`,
                                  `tagId3`,
                                  `newDuration3`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `orgReference`,
                                  `day4`,
                                  `tagId1`,
                                  `newDuration4`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `orgReference`,
                                  `day4`,
                                  `tagId2`,
                                  `newDuration4`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `orgReference`,
                                  `day4`,
                                  `tagId3`,
                                  `newDuration4`) =>
                  ok
              }
            }(any[Writes[BookingByTagId]], any[DBSession]))
    }
  }

  private def testHandleDurationOverSeveralDays(
      eventFactory: BookingV3 => PersistedEvent)(
      verify: (
          BookingByProjectRepository,
          BookingByTagRepository,
          BookingByTypeRepository,
          EntityReference[ProjectId],
          TagId,
          TagId,
          TagId,
          EntityReference[UserId],
          EntityReference[OrganisationId],
          LocalDate,
          LocalDate,
          LocalDate,
          Duration,
          Duration,
          Duration) => MatchResult[_]): WithUserTimeBookingStatisticsViewMock =
    new WithUserTimeBookingStatisticsViewMock {
      val day1: LocalDateTime = LocalDateTime.parse("2000-01-01")
      val day2: LocalDateTime = day1.plusDays(1)
      val day3: LocalDateTime = day1.plusDays(2)
      val stop: DateTime      = day3.plusHours(10).toDateTime
      val start: DateTime     = day1.plusHours(5).toDateTime
      val projectReference: EntityReference[ProjectId] =
        EntityReference(ProjectId(), "proj")
      val tagId1: TagId = TagId("tag1")
      val tagId2: TagId = TagId("tag2")
      val tagId3: TagId = TagId("tag3")

      val tag1: SimpleTag = SimpleTag(tagId1)
      val tag2: GitlabIssueTag = GitlabIssueTag(tagId2,
                                                projectId = 1,
                                                summary = None,
                                                relatedTags =
                                                  Seq(SimpleTag(tagId3)),
                                                issueLink = "")

      val duration1: Duration = Duration.standardHours(24 - 5)
      val duration2: Duration = Duration.standardHours(24)
      val duration3: Duration = Duration.standardHours(10)

      val booking: BookingV3 = BookingV3(
        id = BookingId(),
        start = start.toLocalDateTimeWithZone,
        end = Some(stop.toLocalDateTimeWithZone),
        duration = new Duration(start, stop),
        userReference = userReference,
        organisationReference = orgReference,
        projectReference = projectReference,
        tags = Set(tag1, tag2)
      )

      probe.send(actorRef, InitializeViewLive(userReference, 0))
      probe.expectMsg(JournalReadingViewIsLive)

      probe.send(actorRef, eventFactory(booking))
      probe.expectMsg(Ack)

      verify(
        bookingByProjectRepository,
        bookingByTagRepository,
        bookingByTypeRepository,
        projectReference,
        tagId1,
        tagId2,
        tagId3,
        userReference,
        orgReference,
        day1.toLocalDate,
        day2.toLocalDate,
        day3.toLocalDate,
        duration1,
        duration2,
        duration3
      )
    }

  private def testAddDuration(eventFactory: BookingV3 => PersistedEvent)
      : WithUserTimeBookingStatisticsViewMock = {
    testHandleDurationOfOneDay(eventFactory) {
      (bookingByProjectRepository,
       bookingByTagRepository,
       bookingByTypeRepository,
       projectReference,
       tagId1,
       tagId2,
       tagId3,
       userReference,
       orgReference,
       day,
       duration) =>
        there.was(one(bookingByProjectRepository).add {
          beLike[BookingByProject] {
            case BookingByProject(_,
                                  `userReference`,
                                  `orgReference`,
                                  `day`,
                                  `projectReference`,
                                  `duration`) =>
              ok
          }
        }(any[Writes[BookingByProjectId]], any[DBSession]))

        there.was(one(bookingByTypeRepository).add {
          beLike[BookingByType] {
            case BookingByType(_,
                               `userReference`,
                               `orgReference`,
                               `day`,
                               `ProjectBooking`,
                               `duration`) =>
              ok
          }
        }(any[Writes[BookingByTypeId]], any[DBSession]))

        there.was(three(bookingByTagRepository).add {
          beLike[BookingByTag] {
            case BookingByTag(_,
                              `userReference`,
                              `orgReference`,
                              `day`,
                              `tagId1`,
                              `duration`) =>
              ok
            case BookingByTag(_,
                              `userReference`,
                              `orgReference`,
                              `day`,
                              `tagId2`,
                              `duration`) =>
              ok
            case BookingByTag(_,
                              `userReference`,
                              `orgReference`,
                              `day`,
                              `tagId3`,
                              `duration`) =>
              ok
          }
        }(any[Writes[BookingByTagId]], any[DBSession]))
    }
  }

  private def testRemoveDuration: WithUserTimeBookingStatisticsViewMock = {
    testHandleDurationOfOneDay(booking => UserTimeBookingRemovedV3(booking)) {
      (bookingByProjectRepository,
       bookingByTagRepository,
       bookingByTypeRepository,
       projectReference,
       tagId1,
       tagId2,
       tagId3,
       userReference,
       orgReference,
       day,
       duration) =>
        there.was(one(bookingByProjectRepository).subtract {
          beLike[BookingByProject] {
            case BookingByProject(_,
                                  `userReference`,
                                  `orgReference`,
                                  `day`,
                                  `projectReference`,
                                  `duration`) =>
              ok
          }
        }(any[Writes[BookingByProjectId]], any[DBSession]))

        there.was(one(bookingByTypeRepository).subtract {
          beLike[BookingByType] {
            case BookingByType(_,
                               `userReference`,
                               `orgReference`,
                               `day`,
                               `ProjectBooking`,
                               `duration`) =>
              ok
          }
        }(any[Writes[BookingByTypeId]], any[DBSession]))

        there.was(three(bookingByTagRepository).subtract {
          beLike[BookingByTag] {
            case BookingByTag(_,
                              `userReference`,
                              `orgReference`,
                              `day`,
                              `tagId1`,
                              `duration`) =>
              ok
            case BookingByTag(_,
                              `userReference`,
                              `orgReference`,
                              `day`,
                              `tagId2`,
                              `duration`) =>
              ok
            case BookingByTag(_,
                              `userReference`,
                              `orgReference`,
                              `day`,
                              `tagId3`,
                              `duration`) =>
              ok
          }
        }(any[Writes[BookingByTagId]], any[DBSession]))
    }
  }

  private def testEditDuration: WithUserTimeBookingStatisticsViewMock = {
    val day         = DateTime.parse("2000-01-01")
    val newStop     = day.plusHours(12)
    val newStart    = day.plusHours(2)
    val newDuration = Duration.standardHours(10)

    testHandleDurationOfOneDay(booking =>
      UserTimeBookingEditedV4(
        booking,
        booking.copy(start = newStart.toLocalDateTimeWithZone,
                     end = Some(newStop.toLocalDateTimeWithZone)))) {
      (bookingByProjectRepository,
       bookingByTagRepository,
       bookingByTypeRepository,
       projectReference,
       tagId1,
       tagId2,
       tagId3,
       userReference,
       orgReference,
       day,
       duration) =>
        there.was(one(bookingByProjectRepository).subtract {
          beLike[BookingByProject] {
            case BookingByProject(_,
                                  `userReference`,
                                  `orgReference`,
                                  `day`,
                                  `projectReference`,
                                  `duration`) =>
              ok
          }
        }(any[Writes[BookingByProjectId]], any[DBSession]))

        there.was(one(bookingByTypeRepository).subtract {
          beLike[BookingByType] {
            case BookingByType(_,
                               `userReference`,
                               `orgReference`,
                               `day`,
                               `ProjectBooking`,
                               `duration`) =>
              ok
          }
        }(any[Writes[BookingByTypeId]], any[DBSession]))

        there.was(three(bookingByTagRepository).subtract {
          beLike[BookingByTag] {
            case BookingByTag(_,
                              `userReference`,
                              `orgReference`,
                              `day`,
                              `tagId1`,
                              `duration`) =>
              ok
            case BookingByTag(_,
                              `userReference`,
                              `orgReference`,
                              `day`,
                              `tagId2`,
                              `duration`) =>
              ok
            case BookingByTag(_,
                              `userReference`,
                              `orgReference`,
                              `day`,
                              `tagId3`,
                              `duration`) =>
              ok
          }
        }(any[Writes[BookingByTagId]], any[DBSession]))

        // test adding of new duration
        there.was(one(bookingByProjectRepository).add {
          beLike[BookingByProject] {
            case BookingByProject(_,
                                  `userReference`,
                                  `orgReference`,
                                  `day`,
                                  `projectReference`,
                                  `newDuration`) =>
              ok
          }
        }(any[Writes[BookingByProjectId]], any[DBSession]))

        there.was(one(bookingByTypeRepository).add {
          beLike[BookingByType] {
            case BookingByType(_,
                               `userReference`,
                               `orgReference`,
                               `day`,
                               `ProjectBooking`,
                               `newDuration`) =>
              ok
          }
        }(any[Writes[models.BookingByTypeId]], any[DBSession]))

        there.was(three(bookingByTagRepository).add {
          beLike[BookingByTag] {
            case BookingByTag(_,
                              `userReference`,
                              `orgReference`,
                              `day`,
                              `tagId1`,
                              `newDuration`) =>
              ok
            case BookingByTag(_,
                              `userReference`,
                              `orgReference`,
                              `day`,
                              `tagId2`,
                              `newDuration`) =>
              ok
            case BookingByTag(_,
                              `userReference`,
                              `orgReference`,
                              `day`,
                              `tagId3`,
                              `newDuration`) =>
              ok
          }
        }(any[Writes[BookingByTagId]], any[DBSession]))
    }
  }

  private def testEditDurationAddTag: WithUserTimeBookingStatisticsViewMock = {
    val tagId4 = TagId("tag4")

    testHandleDurationOfOneDay(booking =>
      UserTimeBookingEditedV4(
        booking,
        booking.copy(tags = booking.tags + SimpleTag(tagId4)))) {
      (_,
       bookingByTagRepository,
       _,
       _,
       tagId1,
       tagId2,
       tagId3,
       userReference,
       orgReference,
       day,
       duration) =>
        // test removing of old duration on all old tags
        there.was(three(bookingByTagRepository).subtract {
          beLike[BookingByTag] {
            case BookingByTag(_,
                              `userReference`,
                              `orgReference`,
                              `day`,
                              `tagId1`,
                              `duration`) =>
              ok
            case BookingByTag(_,
                              `userReference`,
                              `orgReference`,
                              `day`,
                              `tagId2`,
                              `duration`) =>
              ok
            case BookingByTag(_,
                              `userReference`,
                              `orgReference`,
                              `day`,
                              `tagId3`,
                              `duration`) =>
              ok
          }
        }(any[Writes[BookingByTagId]], any[DBSession]))

        // test adding of new duration only with new tags
        there.was(
          4.times(bookingByTagRepository)
            .add {
              beLike[BookingByTag] {
                case BookingByTag(_,
                                  `userReference`,
                                  `orgReference`,
                                  `day`,
                                  `tagId1`,
                                  `duration`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `orgReference`,
                                  `day`,
                                  `tagId2`,
                                  `duration`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `orgReference`,
                                  `day`,
                                  `tagId3`,
                                  `duration`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `orgReference`,
                                  `day`,
                                  `tagId4`,
                                  `duration`) =>
                  ok
              }
            }(any[Writes[BookingByTagId]], any[DBSession]))
    }
  }

  private def testEditDurationRemoveTag
      : WithUserTimeBookingStatisticsViewMock = {

    testHandleDurationOfOneDay(booking =>
      UserTimeBookingEditedV4(
        booking,
        booking.copy(tags = booking.tags.filter(_.isInstanceOf[SimpleTag])))) {
      (_,
       bookingByTagRepository,
       _,
       _,
       tagId1,
       tagId2,
       tagId3,
       userReference,
       orgReference,
       day,
       duration) =>
        // test removing of old duration on all old tags
        there.was(three(bookingByTagRepository).subtract {
          beLike[BookingByTag] {
            case BookingByTag(_,
                              `userReference`,
                              `orgReference`,
                              `day`,
                              `tagId1`,
                              `duration`) =>
              ok
            case BookingByTag(_,
                              `userReference`,
                              `orgReference`,
                              `day`,
                              `tagId2`,
                              `duration`) =>
              ok
            case BookingByTag(_,
                              `userReference`,
                              `orgReference`,
                              `day`,
                              `tagId3`,
                              `duration`) =>
              ok
          }
        }(any[Writes[BookingByTagId]], any[DBSession]))

        // test adding of new duration only with new tags
        there.was(
          one(bookingByTagRepository)
            .add {
              beLike[BookingByTag] {
                case BookingByTag(_,
                                  `userReference`,
                                  `orgReference`,
                                  `day`,
                                  `tagId1`,
                                  `duration`) =>
                  ok
              }
            }(any[Writes[BookingByTagId]], any[DBSession]))
    }
  }

  private def testHandleDurationOfOneDay(
      eventFactory: BookingV3 => PersistedEvent)(
      verify: (
          BookingByProjectRepository,
          BookingByTagRepository,
          BookingByTypeRepository,
          EntityReference[ProjectId],
          TagId,
          TagId,
          TagId,
          EntityReference[UserId],
          EntityReference[OrganisationId],
          LocalDate,
          Duration) => MatchResult[_]): WithUserTimeBookingStatisticsViewMock =
    new WithUserTimeBookingStatisticsViewMock {
      val day: LocalDateTime = LocalDateTime.parse("2000-01-01")
      val stop: DateTime     = day.plusHours(10).toDateTime
      val start: DateTime    = stop.minusHours(2).toDateTime
      val projectReference: EntityReference[ProjectId] =
        EntityReference(ProjectId(), "proj")
      val tagId1: TagId      = TagId("tag1")
      val tagId2: TagId      = TagId("tag2")
      val tagId3: TagId      = TagId("tag3")
      val duration: Duration = Duration.standardHours(2)
      val tag1: SimpleTag    = SimpleTag(tagId1)
      val tag2: GitlabIssueTag = GitlabIssueTag(tagId2,
                                                projectId = 1,
                                                summary = None,
                                                relatedTags =
                                                  Seq(SimpleTag(tagId3)),
                                                issueLink = "")

      val booking: BookingV3 = BookingV3(
        id = BookingId(),
        start = start.toLocalDateTimeWithZone,
        end = Some(stop.toLocalDateTimeWithZone),
        duration = new Duration(start, stop),
        userReference = userReference,
        organisationReference = orgReference,
        projectReference = projectReference,
        tags = Set(tag1, tag2)
      )

      probe.send(actorRef, InitializeViewLive(userReference, 0))
      probe.expectMsg(JournalReadingViewIsLive)

      probe.send(actorRef, eventFactory(booking))
      probe.expectMsg(Ack)

      verify(
        bookingByProjectRepository,
        bookingByTagRepository,
        bookingByTypeRepository,
        projectReference,
        tagId1,
        tagId2,
        tagId3,
        userReference,
        orgReference,
        day.toLocalDate,
        duration
      )
    }

  private def testAddDurationWithoutEnd(
      eventFactory: BookingV3 => PersistedEvent)
      : WithUserTimeBookingStatisticsViewMock =
    new WithUserTimeBookingStatisticsViewMock {
      val start: DateTime = DateTime.now().minusHours(2)
      val projectReference: EntityReference[ProjectId] =
        EntityReference(ProjectId(), "proj")
      val tag1: SimpleTag = SimpleTag(TagId("tag1"))
      val tag2: SimpleTag = SimpleTag(TagId("tag2"))

      val booking: BookingV3 = BookingV3(
        id = BookingId(),
        start = start.toLocalDateTimeWithZone,
        end = None,
        duration = new Duration(0),
        userReference = userReference,
        organisationReference = orgReference,
        projectReference = projectReference,
        tags = Set(tag1, tag2)
      )

      probe.send(actorRef, InitializeViewLive(userReference, 0))
      probe.expectMsg(JournalReadingViewIsLive)

      probe.send(actorRef, eventFactory(booking))
      probe.expectMsg(Ack)

      there.was(
        no(bookingByProjectRepository).add(any[BookingByProject])(
          any[Writes[BookingByProjectId]],
          any[DBSession]))
      there.was(
        no(bookingByTagRepository)
          .add(any[BookingByTag])(any[Writes[BookingByTagId]], any[DBSession]))
      there.was(no(bookingByTypeRepository)
        .add(any[BookingByType])(any[Writes[BookingByTypeId]], any[DBSession]))
    }

  trait WithUserTimeBookingStatisticsViewMock
      extends WithPersistentActorTestScope {
    val userReference: EntityReference[UserId] =
      EntityReference(UserId(), "noob")
    val orgReference: EntityReference[OrganisationId] =
      EntityReference(OrganisationId(), "org1")
    val probe: TestProbe = TestProbe()
    val bookingByProjectRepository: BookingByProjectRepository =
      mockAwaitable[BookingByProjectRepository]
    val bookingByTagRepository: BookingByTagMongoRepository =
      mockAwaitable[BookingByTagMongoRepository]
    val bookingByTypeRepository: BookingByTypeMongoRepository =
      mockAwaitable[BookingByTypeMongoRepository]

    val actorRef: ActorRef = system.actorOf(
      UserTimeBookingStatisticsViewMock.props(userReference,
                                              bookingByProjectRepository,
                                              bookingByTagRepository,
                                              bookingByTypeRepository,
                                              reactiveMongoApi))
  }
}

object UserTimeBookingStatisticsViewMock extends Mockito {
  def props(userReference: EntityReference[UserId],
            bookingByProjectRepository: BookingByProjectRepository,
            bookingByTagRepository: BookingByTagRepository,
            bookingByTypeRepository: BookingByTypeRepository,
            reactiveMongoApi: ReactiveMongoApi): Props = {

    val clientReceiver = mock[ClientReceiver]
    val systemServices = mock[SystemServices]

    Props(
      new UserTimeBookingStatisticsView(clientReceiver,
                                        systemServices,
                                        bookingByProjectRepository,
                                        bookingByTagRepository,
                                        bookingByTypeRepository,
                                        userReference,
                                        reactiveMongoApi))
  }
}
