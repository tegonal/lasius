package controllers

import play.api.test.Helpers._
import play.api.test.PlaySpecification
import repositories.UserBookingStatisticsRepositoryComponentMock
import models._
import org.specs2.mock.Mockito
import play.api.test.WithApplication
import org.joda.time.DateTime
import scala.concurrent.Future
import play.api.mvc._
import play.api.test._
import play.api.libs.json._
import org.joda.time.Duration

class TimeBookingStatisticsControllerSpec extends PlaySpecification with Mockito with Results {
  "getAggregatedStatistics" should {
    "empty sequence if no bookingstatistics where found" in new WithApplication {
      val controller = new TimeBookingStatisticsControllerMock()

      controller.bookingByTagRepository.findByUserIdAndRange(any[UserId], any[DateTime], any[DateTime])(any[Format[BookingByTag]]) returns Future.successful(Seq())

      val to = DateTime.now()
      val from = to.minusDays(1)
      val request: Request[Unit] = FakeRequest().withBody("")
      val result: Future[Result] = controller.getAggregatedStatistics("tag", from, to)(request)

      status(result) must equalTo(OK)
      contentAsJson(result) must equalTo(Json.arr())
    }

    "bad request if source was not found" in new WithApplication {
      val controller = new TimeBookingStatisticsControllerMock()

      val to = DateTime.now()
      val from = to.minusDays(1)
      val request: Request[Unit] = FakeRequest().withBody("")
      val result: Future[Result] = controller.getAggregatedStatistics("anysource", from, to)(request)

      status(result) must equalTo(BAD_REQUEST)
    }

    "aggregate statistics grouped by same tagid" in new WithApplication {
      val controller = new TimeBookingStatisticsControllerMock()
      val userId = UserId("")
      val tagId1 = TagId("t1")
      val tagId2 = TagId("t2")
      val day = DateTime.now

      val statistics = Seq(BookingByTag(BookingByTagId(), userId, day, tagId1, Duration.standardHours(2)),
        BookingByTag(BookingByTagId(), userId, day, tagId1, Duration.standardHours(
          3)),
        BookingByTag(BookingByTagId(), userId, day, tagId2, Duration.standardHours(1)),
        BookingByTag(BookingByTagId(), userId, day, tagId2, Duration.standardHours(5)))

      controller.bookingByTagRepository.findByUserIdAndRange(any[UserId], any[DateTime], any[DateTime])(any[Format[BookingByTag]]) returns Future.successful(statistics)

      val to = DateTime.now()
      val from = to.minusDays(1)
      val request: Request[Unit] = FakeRequest().withBody("")
      val result: Future[Result] = controller.getAggregatedStatistics("tag", from, to)(request)
      def hoursToMillis(hours: Int) = hours * 60 * 60 * 1000

      status(result) must equalTo(OK)
      contentAsJson(result) must equalTo(Json.arr(
        Json.obj("label" -> tagId2.value, "value" -> hoursToMillis(6)),
        Json.obj("label" -> tagId1.value, "value" -> hoursToMillis(5))))
    }
  }
}
class TimeBookingStatisticsControllerMock extends SecurityControllerMock with TimeBookingStatisticsController with UserBookingStatisticsRepositoryComponentMock with Controller