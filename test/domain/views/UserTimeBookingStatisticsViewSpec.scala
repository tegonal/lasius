package domain.views

import org.specs2.matcher.Matchers
import org.specs2.matcher.Matchers._
import org.specs2.mutable.Specification
import akka.PersistentActorTestScope
import akka.testkit._
import models.UserId
import actor.ClientReceiverComponentMock
import repositories.UserBookingStatisticsRepositoryComponentMock
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

class UserTimeBookingStatisticsViewSpec extends Specification with Mockito {

  "UserTimeBookingStatisticsView UserTimeBookingInitialized" should {
    "delete collections" in new PersistentActorTestScope {

      val userId = UserId("noob")
      val probe = TestProbe()
      val actorRef = system.actorOf(UserTimeBookingStatisticsViewMock.props(userId))

      probe.send(actorRef, UserTimeBookingInitialized(userId))
      probe.expectMsg(UserTimeBookingStatisticsView.Ack)

      there was one(UserBookingStatisticsRepositoryComponentMock.bookingByCategoryRepository).deleteByUser(isEq(userId))(any[ExecutionContext], any[Format[BookingByCategory]])
      there was one(UserBookingStatisticsRepositoryComponentMock.bookingByProjectRepository).deleteByUser(isEq(userId))(any[ExecutionContext], any[Format[BookingByProject]])
      there was one(UserBookingStatisticsRepositoryComponentMock.bookingByTagRepository).deleteByUser(isEq(userId))(any[ExecutionContext], any[Format[BookingByTag]])
    }
  }

  "UserTimeBookingStatisticsView UserTimeBookingStopped" should {
    "add new duration to stats" in new PersistentActorTestScope {

      val userId = UserId("noob")
      val probe = TestProbe()
      val actorRef = system.actorOf(UserTimeBookingStatisticsViewMock.props(userId))
      val day = DateTime.parse("2000-01-01")
      val stop = day.plusHours(10)
      val start = stop.minusHours(2)
      val categoryId = CategoryId("cat")
      val projectId = ProjectId("proj")
      val tagId1 = TagId("tag1")
      val tagId2 = TagId("tag2")
      val duration = Duration.standardHours(2)

      val booking = Booking(BookingId("b1"), start, Some(stop), userId, categoryId, projectId, Seq(tagId1, tagId2))

      probe.send(actorRef, UserTimeBookingStopped(booking))
      probe.expectMsg(UserTimeBookingStatisticsView.Ack)

      there was one(UserBookingStatisticsRepositoryComponentMock.bookingByCategoryRepository).add {
        beLike[BookingByCategory] {
          case BookingByCategory(_, userId, day, categoryId, duration) => ok
        }
      }(any[Writes[BookingByCategoryId]])

      there was one(UserBookingStatisticsRepositoryComponentMock.bookingByProjectRepository).add {
        beLike[BookingByProject] {
          case BookingByProject(_, userId, day, projectId, duration) => ok
        }
      }(any[Writes[BookingByProjectId]])

      there was two(UserBookingStatisticsRepositoryComponentMock.bookingByTagRepository).add {
        beLike[BookingByTag] {
          case BookingByTag(_, userId, day, tagId1, duration) => ok
          case BookingByTag(_, userId, day, tagId2, duration) => ok
        }
      }(any[Writes[BookingByTagId]])
    }
  }
}

object UserTimeBookingStatisticsViewMock {
  def props(userId: UserId) = Props(classOf[UserTimeBookingStatisticsViewMock], userId)
}

class UserTimeBookingStatisticsViewMock(userId: UserId) extends UserTimeBookingStatisticsView(userId)
  with UserBookingStatisticsRepositoryComponentMock with ClientReceiverComponentMock