/*   __                          __                                          *\
*   / /____ ___ ____  ___  ___ _/ /       lasius                      *
*  / __/ -_) _ `/ _ \/ _ \/ _ `/ /        contributed by tegonal              *
*  \__/\__/\_, /\___/_//_/\_,_/_/         http://tegonal.com/                 *
*         /___/                                                               *
*                                                                             *
* This program is free software: you can redistribute it and/or modify it     *
* under the terms of the GNU General Public License as published by    *
* the Free Software Foundation, either version 3 of the License,              *
* or (at your option) any later version.                                      *
*                                                                             *
* This program is distributed in the hope that it will be useful, but         *
* WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY  *
* or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for *
* more details.                                                               *
*                                                                             *
* You should have received a copy of the GNU General Public License along     *
* with this program. If not, see http://www.gnu.org/licenses/                 *
*                                                                             *
\*                                                                           */
package domain.views

import org.specs2.matcher.Matchers
import org.specs2.matcher.Matchers._
import org.specs2.mutable.Specification
import akka.PersistentActorTestScope
import akka.testkit._
import models.UserId
import actor.ClientReceiverComponentMock
import akka.actor.Props
import akka.pattern.ask
import domain._
import domain.UserTimeBookingAggregate._
import org.specs2.mock.Mockito
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import org.mockito.Matchers.{ argThat, anyInt, eq => isEq }
import models._
import org.joda.time.DateTime
import org.joda.time.Duration
import org.specs2.matcher._
import org.joda.time.LocalTime
import play.api.libs.json._
import repositories.BookingByTagRepository
import repositories.BookingByProjectRepository
import repositories.BookingByCategoryRepository
import repositories.BookingByCategoryRepository
import repositories.BookingByProjectRepository
import repositories.BookingByTagMongoRepository
import repositories.UserBookingStatisticsRepositoryComponent
import domain.AggregateRoot.Event
import akka.actor.ActorSystem
import repositories.BookingByProjectRepository
import org.mockito.verification.VerificationMode
import repositories.BookingByCategoryRepository
import scala.concurrent.Future
import repositories.BookingByCategoryRepository
import repositories.BookingByProjectRepository
import repositories.BookingByTagRepository
import repositories.UserBookingStatisticsRepositoryComponentMock
import repositories.UserBookingStatisticsRepositoryComponentMock
import repositories.UserBookingStatisticsRepositoryComponentMock
import org.joda.time.Interval
import repositories.UserBookingStatisticsRepositoryComponentMockClass
import play.api.Logger

class UserTimeBookingStatisticsViewSpec extends Specification with Mockito {

  "UserTimeBookingStatisticsView UserTimeBookingInitialized" should {
    "delete collections" in new PersistentActorTestScope {

      val userId = UserId("noob")
      val probe = TestProbe()
      val bookingByCategoryRepository = mock[BookingByCategoryRepository]
      val bookingByProjectRepository = mock[BookingByProjectRepository]
      val bookingByTagRepository = mock[BookingByTagMongoRepository]
      val actorRef = system.actorOf(UserTimeBookingStatisticsViewMock.props(userId,
        bookingByCategoryRepository, bookingByProjectRepository, bookingByTagRepository))

      probe.send(actorRef, UserTimeBookingInitialized(userId))
      probe.expectMsg(UserTimeBookingStatisticsView.Ack)

      there was one(bookingByCategoryRepository).deleteByUser(isEq(userId))(any[ExecutionContext], any[Format[BookingByCategory]])
      there was one(bookingByProjectRepository).deleteByUser(isEq(userId))(any[ExecutionContext], any[Format[BookingByProject]])
      there was one(bookingByTagRepository).deleteByUser(isEq(userId))(any[ExecutionContext], any[Format[BookingByTag]])
    }
  }

  "UserTimeBookingStatisticsView UserTimeBookingStopped" should {
    "add new duration to stats" in new PersistentActorTestScope {
      testAddDuration(booking => UserTimeBookingStopped(booking))
    }

    "don't add stats if no enddate is specified" in new PersistentActorTestScope {
      testAddDurationWithoutEnd(booking => UserTimeBookingStopped(booking))
    }
  }

  "UserTimeBookingStatisticsView UserTimeBookingAdded" should {
    "add new duration to stats if end of booking is defined" in new PersistentActorTestScope {
      testAddDuration(booking => UserTimeBookingAdded(booking))
    }

    "don't add stats if no enddate is specified" in new PersistentActorTestScope {
      testAddDurationWithoutEnd(booking => UserTimeBookingAdded(booking))
    }

    "add duration over multiple days" in new PersistentActorTestScope {
      testAddDurationOverSeveralDays(booking => UserTimeBookingAdded(booking))
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

  "UserTimeBookingStatisticsView various cases" should {
    "LAS-24" in new PersistentActorTestScope {
      val userId = UserId("noob")
      val probe = TestProbe()
      val component = new UserBookingStatisticsRepositoryComponentMockClass
      val actorRef = system.actorOf(UserTimeBookingStatisticsViewMock.props(userId, component))

      //days from 23. to 28.
      val day1 = DateTime.parse("2015-04-23")
      val day2 = day1.plusDays(1)
      val day3 = day2.plusDays(1)
      val day4 = day3.plusDays(1)
      val day5 = day4.plusDays(1)
      val day6 = day5.plusDays(1)

      val categoryId = CategoryId("Cat1")
      val projectId1 = ProjectId("Proj1")
      val projectId2 = ProjectId("Proj2")
      val tagId1 = TagId("LAS-22")
      val tagId2 = TagId("Testsystem")

      //booking 1
      val start1 = day1.withHourOfDay(13).withMinuteOfHour(42)
      val end1 = day6.withHourOfDay(9).withMinuteOfHour(17)
      val booking1 = Booking(BookingId("b1"), start1, Some(end1), userId, categoryId, projectId1, Seq(tagId1))

      //booking 2
      val start2 = end1
      val end2 = start2.withHourOfDay(12).withMinuteOfHour(2)
      val booking2 = Booking(BookingId("b2"), start2, Some(end2), userId, categoryId, projectId2, Seq(tagId2))

      //booking 3
      val start3 = start2.withHourOfDay(12).withMinuteOfHour(50)
      val end3 = start2.withHourOfDay(15).withMinuteOfHour(30)
      val booking3 = Booking(BookingId("b3"), start3, Some(end3), userId, categoryId, projectId2, Seq(tagId2))

      //booking 4
      val start4 = start2.withHourOfDay(16).withMinuteOfHour(28)
      val end4 = start2.withHourOfDay(21).withMinuteOfHour(41)
      val booking4 = Booking(BookingId("b4"), start4, Some(end4), userId, categoryId, projectId2, Seq(tagId2))

      //durations
      //whole day
      val dayDuration = Duration.standardHours(24)
      //category durations
      //start of booking 1
      val startCatDuration = Duration.standardHours(24 - 14).plus(Duration.standardMinutes(60 - 42))
      val endCatDuration1 = Duration.standardHours(9).plus(Duration.standardMinutes(17))
      val endCatDuration2 = new Interval(start2, end2).toDuration()
      val endCatDuration3 = new Interval(start3, end3).toDuration()
      val endCatDuration4 = new Interval(start4, end4).toDuration()

      probe.send(actorRef, UserTimeBookingAdded(booking1))
      probe.expectMsg(UserTimeBookingStatisticsView.Ack)
      probe.send(actorRef, UserTimeBookingAdded(booking2))
      probe.expectMsg(UserTimeBookingStatisticsView.Ack)
      probe.send(actorRef, UserTimeBookingAdded(booking3))
      probe.expectMsg(UserTimeBookingStatisticsView.Ack)
      probe.send(actorRef, UserTimeBookingAdded(booking4))
      probe.expectMsg(UserTimeBookingStatisticsView.Ack)

      there was 9.times(component.bookingByCategoryRepository).add {
        beLike[BookingByCategory] {
          case BookingByCategory(_, `userId`, `day1`, `categoryId`, `startCatDuration`) => ok
          case BookingByCategory(_, `userId`, `day2`, `categoryId`, `dayDuration`) => ok
          case BookingByCategory(_, `userId`, `day3`, `categoryId`, `dayDuration`) => ok
          case BookingByCategory(_, `userId`, `day4`, `categoryId`, `dayDuration`) => ok
          case BookingByCategory(_, `userId`, `day5`, `categoryId`, `dayDuration`) => ok
          case BookingByCategory(_, `userId`, `day6`, `categoryId`, `endCatDuration1`) => ok
          case BookingByCategory(_, `userId`, `day6`, `categoryId`, `endCatDuration2`) => ok
          case BookingByCategory(_, `userId`, `day6`, `categoryId`, `endCatDuration3`) => ok
          case BookingByCategory(_, `userId`, `day6`, `categoryId`, `endCatDuration4`) => ok
        }
      }(any[Writes[BookingByCategoryId]])

    }
  }

  def testAddDurationOverSeveralDays(eventFactory: Booking => Event)(implicit system: ActorSystem) = {
    testHandleDurationOverSeveralDays(eventFactory) {
      (bookingByCategoryRepository, bookingByProjectRepository, bookingByTagRepository, categoryId, projectId, tagId1, tagId2, userId, day1, day2, day3, duration1, duration2, duration3) =>
        there was 3.times(bookingByCategoryRepository).add {
          beLike[BookingByCategory] {
            case BookingByCategory(_, `userId`, `day1`, `categoryId`, `duration1`) => ok
            case BookingByCategory(_, `userId`, `day2`, `categoryId`, `duration2`) => ok
            case BookingByCategory(_, `userId`, `day3`, `categoryId`, `duration3`) => ok
          }
        }(any[Writes[BookingByCategoryId]])

        there was 3.times(bookingByProjectRepository).add {
          beLike[BookingByProject] {
            case BookingByProject(_, `userId`, `day1`, `projectId`, `duration1`) => ok
            case BookingByProject(_, `userId`, `day2`, `projectId`, `duration2`) => ok
            case BookingByProject(_, `userId`, `day3`, `projectId`, `duration3`) => ok
          }
        }(any[Writes[BookingByProjectId]])

        there was 6.times(bookingByTagRepository).add {
          beLike[BookingByTag] {
            case BookingByTag(_, `userId`, `day1`, `tagId1`, `duration1`) => ok
            case BookingByTag(_, `userId`, `day1`, `tagId2`, `duration1`) => ok
            case BookingByTag(_, `userId`, `day2`, `tagId1`, `duration2`) => ok
            case BookingByTag(_, `userId`, `day2`, `tagId2`, `duration2`) => ok
            case BookingByTag(_, `userId`, `day3`, `tagId1`, `duration3`) => ok
            case BookingByTag(_, `userId`, `day3`, `tagId2`, `duration3`) => ok
          }
        }(any[Writes[BookingByTagId]])
    }
  }

  def testRemoveDurationOverSeveralDays(implicit system: ActorSystem) = {
    testHandleDurationOverSeveralDays(booking => UserTimeBookingRemoved(booking)) {
      (bookingByCategoryRepository, bookingByProjectRepository, bookingByTagRepository, categoryId, projectId, tagId1, tagId2, userId, day1, day2, day3, duration1, duration2, duration3) =>
        there was 3.times(bookingByCategoryRepository).subtract {
          beLike[BookingByCategory] {
            case BookingByCategory(_, `userId`, `day1`, `categoryId`, `duration1`) => ok
            case BookingByCategory(_, `userId`, `day2`, `categoryId`, `duration2`) => ok
            case BookingByCategory(_, `userId`, `day3`, `categoryId`, `duration3`) => ok
          }
        }(any[Writes[BookingByCategoryId]])

        there was 3.times(bookingByProjectRepository).subtract {
          beLike[BookingByProject] {
            case BookingByProject(_, `userId`, `day1`, `projectId`, `duration1`) => ok
            case BookingByProject(_, `userId`, `day2`, `projectId`, `duration2`) => ok
            case BookingByProject(_, `userId`, `day3`, `projectId`, `duration3`) => ok
          }
        }(any[Writes[BookingByProjectId]])

        there was 6.times(bookingByTagRepository).subtract {
          beLike[BookingByTag] {
            case BookingByTag(_, `userId`, `day1`, `tagId1`, `duration1`) => ok
            case BookingByTag(_, `userId`, `day1`, `tagId2`, `duration1`) => ok
            case BookingByTag(_, `userId`, `day2`, `tagId1`, `duration2`) => ok
            case BookingByTag(_, `userId`, `day2`, `tagId2`, `duration2`) => ok
            case BookingByTag(_, `userId`, `day3`, `tagId1`, `duration3`) => ok
            case BookingByTag(_, `userId`, `day3`, `tagId2`, `duration3`) => ok
          }
        }(any[Writes[BookingByTagId]])
    }
  }

  def testHandleDurationOverSeveralDays(eventFactory: Booking => Event)(verify: (BookingByCategoryRepository, BookingByProjectRepository, BookingByTagRepository, UserId, CategoryId, ProjectId, TagId, TagId, DateTime, DateTime, DateTime, Duration, Duration, Duration) => MatchResult[_])(implicit system: ActorSystem) = {
    val userId = UserId("noob")
    val probe = TestProbe()
    val bookingByCategoryRepository = mock[BookingByCategoryRepository]
    val bookingByProjectRepository = mock[BookingByProjectRepository]
    val bookingByTagRepository = mock[BookingByTagMongoRepository]
    val actorRef = system.actorOf(UserTimeBookingStatisticsViewMock.props(userId,
      bookingByCategoryRepository, bookingByProjectRepository, bookingByTagRepository))
    val day1 = DateTime.parse("2000-01-01")
    val day2 = day1.plusDays(1)
    val day3 = day1.plusDays(2)
    val stop = day3.plusHours(10)
    val start = day1.plusHours(5)
    val categoryId = CategoryId("cat")
    val projectId = ProjectId("proj")
    val tagId1 = TagId("tag1")
    val tagId2 = TagId("tag2")

    val duration1 = Duration.standardHours(24 - 5)
    val duration2 = Duration.standardHours(24)
    val duration3 = Duration.standardHours(10)

    val booking = Booking(BookingId("b1"), start, Some(stop), userId, categoryId, projectId, Seq(tagId1, tagId2))

    probe.send(actorRef, eventFactory(booking))
    probe.expectMsg(UserTimeBookingStatisticsView.Ack)

    verify
  }

  def testAddDuration(eventFactory: Booking => Event)(implicit system: ActorSystem) = {
    testHandleDurationOfOneDay(eventFactory) {
      (bookingByCategoryRepository, bookingByProjectRepository, bookingByTagRepository, categoryId, projectId, tagId1, tagId2, userId, day, duration) =>
        there was one(bookingByCategoryRepository).add {
          beLike[BookingByCategory] {
            case BookingByCategory(_, `userId`, `day`, `categoryId`, `duration`) => ok
          }
        }(any[Writes[BookingByCategoryId]])

        there was one(bookingByProjectRepository).add {
          beLike[BookingByProject] {
            case BookingByProject(_, `userId`, `day`, `projectId`, `duration`) => ok
          }
        }(any[Writes[BookingByProjectId]])

        there was two(bookingByTagRepository).add {
          beLike[BookingByTag] {
            case BookingByTag(_, `userId`, `day`, `tagId1`, `duration`) => ok
            case BookingByTag(_, `userId`, `day`, `tagId2`, `duration`) => ok
          }
        }(any[Writes[BookingByTagId]])
    }
  }

  def testRemoveDuration(implicit system: ActorSystem) = {
    testHandleDurationOfOneDay(booking => UserTimeBookingRemoved(booking)) {
      (bookingByCategoryRepository, bookingByProjectRepository, bookingByTagRepository, categoryId, projectId, tagId1, tagId2, userId, day, duration) =>
        there was one(bookingByCategoryRepository).subtract {
          beLike[BookingByCategory] {
            case BookingByCategory(_, `userId`, `day`, `categoryId`, `duration`) => ok
          }
        }(any[Writes[BookingByCategoryId]])

        there was one(bookingByProjectRepository).subtract {
          beLike[BookingByProject] {
            case BookingByProject(_, `userId`, `day`, `projectId`, `duration`) => ok
          }
        }(any[Writes[BookingByProjectId]])

        there was two(bookingByTagRepository).subtract {
          beLike[BookingByTag] {
            case BookingByTag(_, `userId`, `day`, `tagId1`, `duration`) => ok
            case BookingByTag(_, `userId`, `day`, `tagId2`, `duration`) => ok
          }
        }(any[Writes[BookingByTagId]])
    }
  }

  def testHandleDurationOfOneDay(eventFactory: Booking => Event)(verify: (BookingByCategoryRepository, BookingByProjectRepository, BookingByTagRepository, UserId, CategoryId, ProjectId, TagId, TagId, DateTime, Duration) => MatchResult[_])(implicit system: ActorSystem) = {
    val userId = UserId("noob")
    val probe = TestProbe()
    val bookingByCategoryRepository = mock[BookingByCategoryRepository]
    val bookingByProjectRepository = mock[BookingByProjectRepository]
    val bookingByTagRepository = mock[BookingByTagMongoRepository]
    val actorRef = system.actorOf(UserTimeBookingStatisticsViewMock.props(userId,
      bookingByCategoryRepository, bookingByProjectRepository, bookingByTagRepository))
    val day = DateTime.parse("2000-01-01")
    val stop = day.plusHours(10)
    val start = stop.minusHours(2)
    val categoryId = CategoryId("cat")
    val projectId = ProjectId("proj")
    val tagId1 = TagId("tag1")
    val tagId2 = TagId("tag2")
    val duration = Duration.standardHours(2)

    val booking = Booking(BookingId("b1"), start, Some(stop), userId, categoryId, projectId, Seq(tagId1, tagId2))

    probe.send(actorRef, eventFactory(booking))
    probe.expectMsg(UserTimeBookingStatisticsView.Ack)

    verify
  }

  def testAddDurationWithoutEnd(eventFactory: Booking => Event)(implicit system: ActorSystem) = {
    val userId = UserId("noob")
    val probe = TestProbe()
    val bookingByCategoryRepository = mock[BookingByCategoryRepository]
    val bookingByProjectRepository = mock[BookingByProjectRepository]
    val bookingByTagRepository = mock[BookingByTagMongoRepository]
    val actorRef = system.actorOf(UserTimeBookingStatisticsViewMock.props(userId,
      bookingByCategoryRepository, bookingByProjectRepository, bookingByTagRepository))
    val day = DateTime.parse("2000-01-01")
    val start = DateTime.now().minusHours(2)
    val categoryId = CategoryId("cat")
    val projectId = ProjectId("proj")
    val tagId1 = TagId("tag1")
    val tagId2 = TagId("tag2")
    val duration = Duration.standardHours(2)

    val booking = Booking(BookingId("b1"), start, None, userId, categoryId, projectId, Seq(tagId1, tagId2))

    probe.send(actorRef, eventFactory(booking))
    probe.expectMsg(UserTimeBookingStatisticsView.Ack)

    there was no(bookingByCategoryRepository).add(any[BookingByCategory])(any[Writes[BookingByCategoryId]])
    there was no(bookingByProjectRepository).add(any[BookingByProject])(any[Writes[BookingByProjectId]])
    there was no(bookingByTagRepository).add(any[BookingByTag])(any[Writes[BookingByTagId]])
  }
}

object UserTimeBookingStatisticsViewMock {
  def props(userId: UserId, bookingByCategoryRepository: BookingByCategoryRepository,
    bookingByProjectRepository: BookingByProjectRepository, bookingByTagRepository: BookingByTagRepository) = Props(classOf[UserTimeBookingStatisticsViewMock], userId, bookingByCategoryRepository, bookingByProjectRepository, bookingByTagRepository)

  def props(userId: UserId, comp: UserBookingStatisticsRepositoryComponentMock) = Props(classOf[UserTimeBookingStatisticsViewMock], userId,
    comp.bookingByCategoryRepository,
    comp.bookingByProjectRepository,
    comp.bookingByTagRepository)
}

class UserTimeBookingStatisticsViewMock(userId: UserId, val bookingByCategoryRepository: BookingByCategoryRepository,
  val bookingByProjectRepository: BookingByProjectRepository, val bookingByTagRepository: BookingByTagRepository) extends UserTimeBookingStatisticsView(userId)
  with UserBookingStatisticsRepositoryComponent with ClientReceiverComponentMock