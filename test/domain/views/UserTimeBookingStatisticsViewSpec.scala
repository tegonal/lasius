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
import play.api.libs.json.Format
import models.BookingByCategory
import org.mockito.Matchers.{ argThat, anyInt, eq => isEq }
import models.BookingByProject
import models.BookingByTag

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
}

object UserTimeBookingStatisticsViewMock {
  def props(userId: UserId) = Props(classOf[UserTimeBookingStatisticsViewMock], userId)
}

class UserTimeBookingStatisticsViewMock(userId: UserId) extends UserTimeBookingStatisticsView(userId)
  with UserBookingStatisticsRepositoryComponentMock with ClientReceiverComponentMock