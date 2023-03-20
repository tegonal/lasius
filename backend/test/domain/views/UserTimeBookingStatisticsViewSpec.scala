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
import akka.actor.{ActorSystem, Props}
import akka.pattern.StatusReply.Ack
import akka.testkit._
import core.{DBSession, SystemServices}
import domain.AggregateRoot.{InitializeViewLive, RestoreViewFromState}
import domain.UserTimeBookingAggregate.UserTimeBooking
import models.LocalDateTimeWithTimeZone.DateTimeHelper
import models._
import mongo.EmbedMongo
import org.joda.time._
import org.specs2.matcher._
import org.specs2.mock._
import org.specs2.mock.mockito.MockitoMatchers
import org.specs2.mutable._
import util.MockAwaitable
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoApi
import repositories._

class UserTimeBookingStatisticsViewSpec
    extends Specification
    with Mockito
    with MockAwaitable
    with MockitoMatchers
    with EmbedMongo {
  sequential

  "UserTimeBookingStatisticsView UserTimeBookingInitialized" should {
    "delete collections" in new PersistentActorTestScope {

      val userReference =
        EntityReference(UserId(), "noob")
      val probe                      = TestProbe()
      val bookingByProjectRepository = mockAwaitable[BookingByProjectRepository]
      val bookingByTagRepository = mockAwaitable[BookingByTagMongoRepository]

      val actorRef = system.actorOf(
        UserTimeBookingStatisticsViewMock.props(userReference,
                                                bookingByProjectRepository,
                                                bookingByTagRepository,
                                                reactiveMongoApi))

      probe.send(actorRef,
                 RestoreViewFromState(userReference,
                                      0,
                                      UserTimeBooking(userReference, Seq())))
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
    "add new duration to stats" in new PersistentActorTestScope {
      testAddDuration(booking => UserTimeBookingStoppedV2(booking))
    }

    "don't add stats if no enddate is specified" in new PersistentActorTestScope {
      testAddDurationWithoutEnd(booking => UserTimeBookingStoppedV2(booking))
    }
  }

  "UserTimeBookingStatisticsView UserTimeBookingAdded" should {
    "add new duration to stats if end of booking is defined" in new PersistentActorTestScope {
      testAddDuration(booking => UserTimeBookingAddedV2(booking))
    }

    "don't add stats if no enddate is specified" in new PersistentActorTestScope {
      testAddDurationWithoutEnd(booking => UserTimeBookingAddedV2(booking))
    }

    "add duration over multiple days" in new PersistentActorTestScope {
      testAddDurationOverSeveralDays(booking => UserTimeBookingAddedV2(booking))
    }
  }

  "UserTimeBookingStatisticsView UserTimeBookingRemoved" should {
    "remove duration from total of same day" in new PersistentActorTestScope {
      testRemoveDuration
    }

    "remove duration over multiple days" in new PersistentActorTestScope {
      testRemoveDurationOverSeveralDays
    }
  }

  "UserTimeBookingStatisticsView UserTimeBookingEdited" should {
    "edit duration from total of same day" in new PersistentActorTestScope {
      testEditDuration
    }

    "edit duration over multiple days" in new PersistentActorTestScope {
      testEditDurationOverSeveralDays
    }

    "add tag" in new PersistentActorTestScope {
      testEditDurationAddTag
    }

    "remove tag" in new PersistentActorTestScope {
      testEditDurationRemoveTag
    }
  }

  "UserTimeBookingStatisticsView UserTimeBookingStartTimeChanged" should {
    "do nothing" in new PersistentActorTestScope {
      val userReference =
        EntityReference(UserId(), "noob")
      val probe                      = TestProbe()
      val bookingByProjectRepository = mockAwaitable[BookingByProjectRepository]
      val bookingByTagRepository = mockAwaitable[BookingByTagMongoRepository]
      val actorRef = system.actorOf(
        UserTimeBookingStatisticsViewMock.props(userReference,
                                                bookingByProjectRepository,
                                                bookingByTagRepository,
                                                reactiveMongoApi))
      val day       = DateTime.parse("2000-01-01")
      val stop      = day.plusHours(10)
      val start     = day.plusHours(5)
      val bookingId = BookingId()
      val newStart  = start.minusHours(3)

      val duration1 = Duration.standardHours(24 - 5)
      val duration2 = Duration.standardHours(24)
      val duration3 = Duration.standardHours(10)

      probe.send(actorRef, InitializeViewLive(userReference, 0))
      probe.expectMsg(JournalReadingViewIsLive)

      probe.send(actorRef,
                 UserTimeBookingStartTimeChanged(bookingId, start, newStart))
      probe.expectMsg(Ack)

      there.was(noCallsTo(bookingByProjectRepository))
      there.was(noCallsTo(bookingByTagRepository))
    }
  }

  "UserTimeBookingStatisticsView various cases" should {
    "LAS-24" in new PersistentActorTestScope {
      val userReference =
        EntityReference(UserId(), "noob")
      val probe                      = TestProbe()
      val bookingByProjectRepository = mockAwaitable[BookingByProjectRepository]
      val bookingByTagRepository = mockAwaitable[BookingByTagMongoRepository]
      val actorRef = system.actorOf(
        UserTimeBookingStatisticsViewMock.props(userReference,
                                                bookingByProjectRepository,
                                                bookingByTagRepository,
                                                reactiveMongoApi))

      probe.send(actorRef, InitializeViewLive(userReference, 0))
      probe.expectMsg(JournalReadingViewIsLive)

      // days from 23. to 28.
      val day1 = DateTime.parse("2015-04-23")
      val day2 = day1.plusDays(1)
      val day3 = day2.plusDays(1)
      val day4 = day3.plusDays(1)
      val day5 = day4.plusDays(1)
      val day6 = day5.plusDays(1)

      val projectId1 =
        EntityReference(ProjectId(), "Proj1")
      val projectId2 =
        EntityReference(ProjectId(), "Proj2")
      val teamReference =
        EntityReference(OrganisationId(), "Team1")
      val tag1 = SimpleTag(TagId("LAS-22"))
      val tag2 = SimpleTag(TagId("Testsystem"))

      // booking 1
      val start1 =
        day1.withHourOfDay(13).withMinuteOfHour(42)
      val end1 =
        day6.withHourOfDay(9).withMinuteOfHour(17)

      val booking1 = BookingV2(BookingId(),
                               start1.toLocalDateTimeWithZone(),
                               Some(end1.toLocalDateTimeWithZone()),
                               userReference,
                               teamReference,
                               projectId1,
                               Set(tag1))

      // booking 2
      val start2 = end1
      val end2   = start2.withHourOfDay(12).withMinuteOfHour(2)
      val booking2 = BookingV2(BookingId(),
                               start2.toLocalDateTimeWithZone(),
                               Some(end2.toLocalDateTimeWithZone()),
                               userReference,
                               teamReference,
                               projectId2,
                               Set(tag2))

      // booking 3
      val start3 = start2.withHourOfDay(12).withMinuteOfHour(50)
      val end3   = start2.withHourOfDay(15).withMinuteOfHour(30)
      val booking3 = BookingV2(BookingId(),
                               start3.toLocalDateTimeWithZone(),
                               Some(end3.toLocalDateTimeWithZone()),
                               userReference,
                               teamReference,
                               projectId2,
                               Set(tag2))

      // booking 4
      val start4 = start2.withHourOfDay(16).withMinuteOfHour(28)
      val end4   = start2.withHourOfDay(21).withMinuteOfHour(41)
      val booking4 = BookingV2(BookingId(),
                               start4.toLocalDateTimeWithZone(),
                               Some(end4.toLocalDateTimeWithZone()),
                               userReference,
                               teamReference,
                               projectId2,
                               Set(tag2))

      // durations
      // whole day
      val dayDuration = Duration.standardHours(24)
      // category durations
      // start of booking 1
      val startCatDuration =
        Duration.standardHours(24 - 14).plus(Duration.standardMinutes(60 - 42))
      val endCatDuration1 =
        Duration.standardHours(9).plus(Duration.standardMinutes(17))
      val endCatDuration2 = new Interval(start2, end2).toDuration()
      val endCatDuration3 = new Interval(start3, end3).toDuration()
      val endCatDuration4 = new Interval(start4, end4).toDuration()

      probe.send(actorRef, UserTimeBookingAddedV2(booking1))
      probe.expectMsg(Ack)
      probe.send(actorRef, UserTimeBookingAddedV2(booking2))
      probe.expectMsg(Ack)
      probe.send(actorRef, UserTimeBookingAddedV2(booking3))
      probe.expectMsg(Ack)
      probe.send(actorRef, UserTimeBookingAddedV2(booking4))
      probe.expectMsg(Ack)
    }
  }

  def testAddDurationOverSeveralDays(eventFactory: BookingV2 => PersistedEvent)(
      implicit system: ActorSystem) = {
    testHandleDurationOverSeveralDays(eventFactory) {
      (bookingByProjectRepository,
       bookingByTagRepository,
       projectReference,
       tagId1,
       tagId2,
       tagId3,
       userReference,
       teamReference,
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
                                      `teamReference`,
                                      `day1`,
                                      `projectReference`,
                                      `duration1`) =>
                  ok
                case BookingByProject(_,
                                      `userReference`,
                                      `teamReference`,
                                      `day2`,
                                      `projectReference`,
                                      `duration2`) =>
                  ok
                case BookingByProject(_,
                                      `userReference`,
                                      `teamReference`,
                                      `day3`,
                                      `projectReference`,
                                      `duration3`) =>
                  ok
              }
            }(any[Writes[BookingByProjectId]], any[DBSession]))

        there.was(
          9.times(bookingByTagRepository)
            .add {
              beLike[BookingByTag] {
                case BookingByTag(_,
                                  `userReference`,
                                  `teamReference`,
                                  `day1`,
                                  `tagId1`,
                                  `duration1`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `teamReference`,
                                  `day1`,
                                  `tagId2`,
                                  `duration1`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `teamReference`,
                                  `day1`,
                                  `tagId3`,
                                  `duration1`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `teamReference`,
                                  `day2`,
                                  `tagId1`,
                                  `duration2`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `teamReference`,
                                  `day2`,
                                  `tagId2`,
                                  `duration2`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `teamReference`,
                                  `day2`,
                                  `tagId3`,
                                  `duration2`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `teamReference`,
                                  `day3`,
                                  `tagId1`,
                                  `duration3`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `teamReference`,
                                  `day3`,
                                  `tagId2`,
                                  `duration3`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `teamReference`,
                                  `day3`,
                                  `tagId3`,
                                  `duration3`) =>
                  ok
              }
            }(any[Writes[BookingByTagId]], any[DBSession]))
    }
  }

  def testRemoveDurationOverSeveralDays(implicit system: ActorSystem) = {
    testHandleDurationOverSeveralDays(booking =>
      UserTimeBookingRemovedV2(booking)) {
      (bookingByProjectRepository,
       bookingByTagRepository,
       projectReference,
       tagId1,
       tagId2,
       tagId3,
       userReference,
       teamReference,
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
                                      `teamReference`,
                                      `day1`,
                                      `projectReference`,
                                      `duration1`) =>
                  ok
                case BookingByProject(_,
                                      `userReference`,
                                      `teamReference`,
                                      `day2`,
                                      `projectReference`,
                                      `duration2`) =>
                  ok
                case BookingByProject(_,
                                      `userReference`,
                                      `teamReference`,
                                      `day3`,
                                      `projectReference`,
                                      `duration3`) =>
                  ok
              }
            }(any[Writes[BookingByProjectId]], any[DBSession]))

        there.was(
          9.times(bookingByTagRepository)
            .subtract {
              beLike[BookingByTag] {
                case BookingByTag(_,
                                  `userReference`,
                                  `teamReference`,
                                  `day1`,
                                  `tagId1`,
                                  `duration1`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `teamReference`,
                                  `day1`,
                                  `tagId2`,
                                  `duration1`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `teamReference`,
                                  `day1`,
                                  `tagId3`,
                                  `duration1`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `teamReference`,
                                  `day2`,
                                  `tagId1`,
                                  `duration2`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `teamReference`,
                                  `day2`,
                                  `tagId2`,
                                  `duration2`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `teamReference`,
                                  `day2`,
                                  `tagId3`,
                                  `duration2`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `teamReference`,
                                  `day3`,
                                  `tagId1`,
                                  `duration3`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `teamReference`,
                                  `day3`,
                                  `tagId2`,
                                  `duration3`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `teamReference`,
                                  `day3`,
                                  `tagId3`,
                                  `duration3`) =>
                  ok
              }
            }(any[Writes[BookingByTagId]], any[DBSession]))
    }
  }

  def testEditDurationOverSeveralDays(implicit system: ActorSystem) = {
    val day1              = LocalDateTime.parse("2000-01-01")
    val day4LocalDateTime = day1.plusDays(3)
    val day4              = day4LocalDateTime.toLocalDate
    val newStop           = day4LocalDateTime.plusHours(12).toDateTime
    val newStart          = day1.plusHours(1).toDateTime

    val newDuration1 = Duration.standardHours(24 - 1)
    val newDuration3 = Duration.standardHours(24)
    val newDuration4 = Duration.standardHours(12)

    testHandleDurationOverSeveralDays(booking =>
      UserTimeBookingEditedV3(
        booking,
        booking.copy(start = newStart.toLocalDateTimeWithZone(),
                     end = Some(newStop.toLocalDateTimeWithZone())))) {
      (bookingByProjectRepository,
       bookingByTagRepository,
       projectReference,
       tagId1,
       tagId2,
       tagId3,
       userReference,
       teamReference,
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
                                      `teamReference`,
                                      `day1`,
                                      `projectReference`,
                                      `duration1`) =>
                  ok
                case BookingByProject(_,
                                      `userReference`,
                                      `teamReference`,
                                      `day2`,
                                      `projectReference`,
                                      `duration2`) =>
                  ok
                case BookingByProject(_,
                                      `userReference`,
                                      `teamReference`,
                                      `day3`,
                                      `projectReference`,
                                      `duration3`) =>
                  ok
              }
            }(any[Writes[BookingByProjectId]], any[DBSession]))

        there.was(
          9.times(bookingByTagRepository)
            .subtract {
              beLike[BookingByTag] {
                case BookingByTag(_,
                                  `userReference`,
                                  `teamReference`,
                                  `day1`,
                                  `tagId1`,
                                  `duration1`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `teamReference`,
                                  `day1`,
                                  `tagId2`,
                                  `duration1`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `teamReference`,
                                  `day1`,
                                  `tagId3`,
                                  `duration1`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `teamReference`,
                                  `day2`,
                                  `tagId1`,
                                  `duration2`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `teamReference`,
                                  `day2`,
                                  `tagId2`,
                                  `duration2`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `teamReference`,
                                  `day2`,
                                  `tagId3`,
                                  `duration2`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `teamReference`,
                                  `day3`,
                                  `tagId1`,
                                  `duration3`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `teamReference`,
                                  `day3`,
                                  `tagId2`,
                                  `duration3`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `teamReference`,
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
                                      `teamReference`,
                                      `day1`,
                                      `projectReference`,
                                      `newDuration1`) =>
                  ok
                case BookingByProject(_,
                                      `userReference`,
                                      `teamReference`,
                                      `day2`,
                                      `projectReference`,
                                      `duration2`) =>
                  ok
                case BookingByProject(_,
                                      `userReference`,
                                      `teamReference`,
                                      `day3`,
                                      `projectReference`,
                                      `newDuration3`) =>
                  ok
                case BookingByProject(_,
                                      `userReference`,
                                      `teamReference`,
                                      `day4`,
                                      `projectReference`,
                                      `newDuration4`) =>
                  ok
              }
            }(any[Writes[BookingByProjectId]], any[DBSession]))

        there.was(
          12.times(bookingByTagRepository)
            .add {
              beLike[BookingByTag] {
                case BookingByTag(_,
                                  `userReference`,
                                  `teamReference`,
                                  `day1`,
                                  `tagId1`,
                                  `newDuration1`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `teamReference`,
                                  `day1`,
                                  `tagId2`,
                                  `newDuration1`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `teamReference`,
                                  `day1`,
                                  `tagId3`,
                                  `newDuration1`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `teamReference`,
                                  `day2`,
                                  `tagId1`,
                                  `duration2`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `teamReference`,
                                  `day2`,
                                  `tagId2`,
                                  `duration2`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `teamReference`,
                                  `day2`,
                                  `tagId3`,
                                  `duration2`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `teamReference`,
                                  `day3`,
                                  `tagId1`,
                                  `newDuration3`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `teamReference`,
                                  `day3`,
                                  `tagId2`,
                                  `newDuration3`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `teamReference`,
                                  `day3`,
                                  `tagId3`,
                                  `newDuration3`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `teamReference`,
                                  `day4`,
                                  `tagId1`,
                                  `newDuration4`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `teamReference`,
                                  `day4`,
                                  `tagId2`,
                                  `newDuration4`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `teamReference`,
                                  `day4`,
                                  `tagId3`,
                                  `newDuration4`) =>
                  ok
              }
            }(any[Writes[BookingByTagId]], any[DBSession]))
    }
  }

  def testHandleDurationOverSeveralDays(
      eventFactory: BookingV2 => PersistedEvent)(
      verify: (BookingByProjectRepository,
               BookingByTagRepository,
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
               Duration) => MatchResult[_])(implicit system: ActorSystem) = {
    val userReference = EntityReference(UserId(), "noob")
    val teamReference =
      EntityReference(OrganisationId(), "team1")
    val probe                      = TestProbe()
    val bookingByProjectRepository = mockAwaitable[BookingByProjectRepository]
    val bookingByTagRepository     = mockAwaitable[BookingByTagMongoRepository]
    val actorRef = system.actorOf(
      UserTimeBookingStatisticsViewMock
        .props(userReference,
               bookingByProjectRepository,
               bookingByTagRepository,
               reactiveMongoApi))
    val day1  = LocalDateTime.parse("2000-01-01")
    val day2  = day1.plusDays(1)
    val day3  = day1.plusDays(2)
    val stop  = day3.plusHours(10).toDateTime
    val start = day1.plusHours(5).toDateTime
    val projectReference =
      EntityReference(ProjectId(), "proj")
    val tagId1 = TagId("tag1")
    val tagId2 = TagId("tag2")
    val tagId3 = TagId("tag3")

    val tag1 = SimpleTag(tagId1)
    val tag2 = GitlabIssueTag(tagId2,
                              projectId = 1,
                              summary = None,
                              relatedTags = Seq(SimpleTag(tagId3)),
                              issueLink = "")

    val duration1 = Duration.standardHours(24 - 5)
    val duration2 = Duration.standardHours(24)
    val duration3 = Duration.standardHours(10)

    val booking = BookingV2(BookingId(),
                            start.toLocalDateTimeWithZone(),
                            Some(stop.toLocalDateTimeWithZone()),
                            userReference,
                            teamReference,
                            projectReference,
                            Set(tag1, tag2))

    probe.send(actorRef, InitializeViewLive(userReference, 0))
    probe.expectMsg(JournalReadingViewIsLive)

    probe.send(actorRef, eventFactory(booking))
    probe.expectMsg(Ack)

    verify(
      bookingByProjectRepository,
      bookingByTagRepository,
      projectReference,
      tagId1,
      tagId2,
      tagId3,
      userReference,
      teamReference,
      day1.toLocalDate,
      day2.toLocalDate,
      day3.toLocalDate,
      duration1,
      duration2,
      duration3
    )
  }

  def testAddDuration(eventFactory: BookingV2 => PersistedEvent)(implicit
      system: ActorSystem) = {
    testHandleDurationOfOneDay(eventFactory) {
      (bookingByProjectRepository,
       bookingByTagRepository,
       projectReference,
       tagId1,
       tagId2,
       tagId3,
       userReference,
       teamReference,
       day,
       duration) =>
        there.was(one(bookingByProjectRepository).add {
          beLike[BookingByProject] {
            case BookingByProject(_,
                                  `userReference`,
                                  `teamReference`,
                                  `day`,
                                  `projectReference`,
                                  `duration`) =>
              ok
          }
        }(any[Writes[BookingByProjectId]], any[DBSession]))

        there.was(three(bookingByTagRepository).add {
          beLike[BookingByTag] {
            case BookingByTag(_,
                              `userReference`,
                              `teamReference`,
                              `day`,
                              `tagId1`,
                              `duration`) =>
              ok
            case BookingByTag(_,
                              `userReference`,
                              `teamReference`,
                              `day`,
                              `tagId2`,
                              `duration`) =>
              ok
            case BookingByTag(_,
                              `userReference`,
                              `teamReference`,
                              `day`,
                              `tagId3`,
                              `duration`) =>
              ok
          }
        }(any[Writes[BookingByTagId]], any[DBSession]))
    }
  }

  def testRemoveDuration(implicit system: ActorSystem) = {
    testHandleDurationOfOneDay(booking => UserTimeBookingRemovedV2(booking)) {
      (bookingByProjectRepository,
       bookingByTagRepository,
       projectReference,
       tagId1,
       tagId2,
       tagId3,
       userReference,
       teamReference,
       day,
       duration) =>
        there.was(one(bookingByProjectRepository).subtract {
          beLike[BookingByProject] {
            case BookingByProject(_,
                                  `userReference`,
                                  `teamReference`,
                                  `day`,
                                  `projectReference`,
                                  `duration`) =>
              ok
          }
        }(any[Writes[BookingByProjectId]], any[DBSession]))

        there.was(three(bookingByTagRepository).subtract {
          beLike[BookingByTag] {
            case BookingByTag(_,
                              `userReference`,
                              `teamReference`,
                              `day`,
                              `tagId1`,
                              `duration`) =>
              ok
            case BookingByTag(_,
                              `userReference`,
                              `teamReference`,
                              `day`,
                              `tagId2`,
                              `duration`) =>
              ok
            case BookingByTag(_,
                              `userReference`,
                              `teamReference`,
                              `day`,
                              `tagId3`,
                              `duration`) =>
              ok
          }
        }(any[Writes[BookingByTagId]], any[DBSession]))
    }
  }

  def testEditDuration(implicit system: ActorSystem) = {
    val day         = DateTime.parse("2000-01-01")
    val newStop     = day.plusHours(12)
    val newStart    = day.plusHours(2)
    val newDuration = Duration.standardHours(10)

    testHandleDurationOfOneDay(booking =>
      UserTimeBookingEditedV3(
        booking,
        booking.copy(start = newStart.toLocalDateTimeWithZone(),
                     end = Some(newStop.toLocalDateTimeWithZone())))) {
      (bookingByProjectRepository,
       bookingByTagRepository,
       projectReference,
       tagId1,
       tagId2,
       tagId3,
       userReference,
       teamReference,
       day,
       duration) =>
        there.was(one(bookingByProjectRepository).subtract {
          beLike[BookingByProject] {
            case BookingByProject(_,
                                  `userReference`,
                                  `teamReference`,
                                  `day`,
                                  `projectReference`,
                                  `duration`) =>
              ok
          }
        }(any[Writes[BookingByProjectId]], any[DBSession]))

        there.was(three(bookingByTagRepository).subtract {
          beLike[BookingByTag] {
            case BookingByTag(_,
                              `userReference`,
                              `teamReference`,
                              `day`,
                              `tagId1`,
                              `duration`) =>
              ok
            case BookingByTag(_,
                              `userReference`,
                              `teamReference`,
                              `day`,
                              `tagId2`,
                              `duration`) =>
              ok
            case BookingByTag(_,
                              `userReference`,
                              `teamReference`,
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
                                  `teamReference`,
                                  `day`,
                                  `projectReference`,
                                  `newDuration`) =>
              ok
          }
        }(any[Writes[BookingByProjectId]], any[DBSession]))

        there.was(three(bookingByTagRepository).add {
          beLike[BookingByTag] {
            case BookingByTag(_,
                              `userReference`,
                              `teamReference`,
                              `day`,
                              `tagId1`,
                              `newDuration`) =>
              ok
            case BookingByTag(_,
                              `userReference`,
                              `teamReference`,
                              `day`,
                              `tagId2`,
                              `newDuration`) =>
              ok
            case BookingByTag(_,
                              `userReference`,
                              `teamReference`,
                              `day`,
                              `tagId3`,
                              `newDuration`) =>
              ok
          }
        }(any[Writes[BookingByTagId]], any[DBSession]))
    }
  }

  def testEditDurationAddTag(implicit system: ActorSystem) = {
    val tagId4 = TagId("tag4")

    testHandleDurationOfOneDay(booking =>
      UserTimeBookingEditedV3(
        booking,
        booking.copy(tags = booking.tags + SimpleTag(tagId4)))) {
      (_,
       bookingByTagRepository,
       _,
       tagId1,
       tagId2,
       tagId3,
       userReference,
       teamReference,
       day,
       duration) =>
        // test removing of old duration on all old tags
        there.was(three(bookingByTagRepository).subtract {
          beLike[BookingByTag] {
            case BookingByTag(_,
                              `userReference`,
                              `teamReference`,
                              `day`,
                              `tagId1`,
                              `duration`) =>
              ok
            case BookingByTag(_,
                              `userReference`,
                              `teamReference`,
                              `day`,
                              `tagId2`,
                              `duration`) =>
              ok
            case BookingByTag(_,
                              `userReference`,
                              `teamReference`,
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
                                  `teamReference`,
                                  `day`,
                                  `tagId1`,
                                  `duration`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `teamReference`,
                                  `day`,
                                  `tagId2`,
                                  `duration`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `teamReference`,
                                  `day`,
                                  `tagId3`,
                                  `duration`) =>
                  ok
                case BookingByTag(_,
                                  `userReference`,
                                  `teamReference`,
                                  `day`,
                                  `tagId4`,
                                  `duration`) =>
                  ok
              }
            }(any[Writes[BookingByTagId]], any[DBSession]))
    }
  }

  def testEditDurationRemoveTag(implicit system: ActorSystem) = {

    testHandleDurationOfOneDay(booking =>
      UserTimeBookingEditedV3(
        booking,
        booking.copy(tags = booking.tags.filter(_.isInstanceOf[SimpleTag])))) {
      (_,
       bookingByTagRepository,
       _,
       tagId1,
       tagId2,
       tagId3,
       userReference,
       teamReference,
       day,
       duration) =>
        // test removing of old duration on all old tags
        there.was(three(bookingByTagRepository).subtract {
          beLike[BookingByTag] {
            case BookingByTag(_,
                              `userReference`,
                              `teamReference`,
                              `day`,
                              `tagId1`,
                              `duration`) =>
              ok
            case BookingByTag(_,
                              `userReference`,
                              `teamReference`,
                              `day`,
                              `tagId2`,
                              `duration`) =>
              ok
            case BookingByTag(_,
                              `userReference`,
                              `teamReference`,
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
                                  `teamReference`,
                                  `day`,
                                  `tagId1`,
                                  `duration`) =>
                  ok
              }
            }(any[Writes[BookingByTagId]], any[DBSession]))
    }
  }

  def testHandleDurationOfOneDay(eventFactory: BookingV2 => PersistedEvent)(
      verify: (BookingByProjectRepository,
               BookingByTagRepository,
               EntityReference[ProjectId],
               TagId,
               TagId,
               TagId,
               EntityReference[UserId],
               EntityReference[OrganisationId],
               LocalDate,
               Duration) => MatchResult[_])(implicit system: ActorSystem) = {
    val userReference = EntityReference(UserId(), "noob")
    val teamReference =
      EntityReference(OrganisationId(), "team1")
    val probe                      = TestProbe()
    val bookingByProjectRepository = mockAwaitable[BookingByProjectRepository]
    val bookingByTagRepository     = mockAwaitable[BookingByTagMongoRepository]
    val actorRef = system.actorOf(
      UserTimeBookingStatisticsViewMock
        .props(userReference,
               bookingByProjectRepository,
               bookingByTagRepository,
               reactiveMongoApi))
    val day   = LocalDateTime.parse("2000-01-01")
    val stop  = day.plusHours(10).toDateTime
    val start = stop.minusHours(2).toDateTime
    val projectReference =
      EntityReference(ProjectId(), "proj")
    val tagId1   = TagId("tag1")
    val tagId2   = TagId("tag2")
    val tagId3   = TagId("tag3")
    val duration = Duration.standardHours(2)
    val tag1     = SimpleTag(tagId1)
    val tag2 = GitlabIssueTag(tagId2,
                              projectId = 1,
                              summary = None,
                              relatedTags = Seq(SimpleTag(tagId3)),
                              issueLink = "")

    val booking = BookingV2(BookingId(),
                            start.toLocalDateTimeWithZone(),
                            Some(stop.toLocalDateTimeWithZone()),
                            userReference,
                            teamReference,
                            projectReference,
                            Set(tag1, tag2))

    probe.send(actorRef, InitializeViewLive(userReference, 0))
    probe.expectMsg(JournalReadingViewIsLive)

    probe.send(actorRef, eventFactory(booking))
    probe.expectMsg(Ack)

    verify(bookingByProjectRepository,
           bookingByTagRepository,
           projectReference,
           tagId1,
           tagId2,
           tagId3,
           userReference,
           teamReference,
           day.toLocalDate,
           duration)
  }

  def testAddDurationWithoutEnd(eventFactory: BookingV2 => PersistedEvent)(
      implicit system: ActorSystem) = {
    val userReference = EntityReference(UserId(), "noob")
    val teamReference =
      EntityReference(OrganisationId(), "team1")
    val probe                      = TestProbe()
    val bookingByProjectRepository = mockAwaitable[BookingByProjectRepository]
    val bookingByTagRepository     = mockAwaitable[BookingByTagMongoRepository]
    val actorRef = system.actorOf(
      UserTimeBookingStatisticsViewMock
        .props(userReference,
               bookingByProjectRepository,
               bookingByTagRepository,
               reactiveMongoApi))
    val start = DateTime.now().minusHours(2)
    val projectReference =
      EntityReference(ProjectId(), "proj")
    val tag1 = SimpleTag(TagId("tag1"))
    val tag2 = SimpleTag(TagId("tag2"))

    val booking = BookingV2(BookingId(),
                            start.toLocalDateTimeWithZone(),
                            None,
                            userReference,
                            teamReference,
                            projectReference,
                            Set(tag1, tag2))

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
  }
}

object UserTimeBookingStatisticsViewMock extends Mockito {
  def props(userReference: EntityReference[UserId],
            bookingByProjectRepository: BookingByProjectRepository,
            bookingByTagRepository: BookingByTagRepository,
            reactiveMongoApi: ReactiveMongoApi) = {

    val clientReceiver = mock[ClientReceiver]
    val systemServices = mock[SystemServices]

    Props(
      classOf[UserTimeBookingStatisticsView],
      clientReceiver,
      systemServices,
      bookingByProjectRepository,
      bookingByTagRepository,
      userReference,
      reactiveMongoApi
    )
  }
}
