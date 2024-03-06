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

package controllers

import core.{
  DBSession,
  MockCache,
  MockCacheAware,
  SystemServices,
  TestApplication
}
import models._
import mongo.EmbedMongo
import org.joda.time.{DateTime, Duration, LocalDate, LocalDateTime}
import org.specs2.mock.Mockito
import org.specs2.mock.mockito.MockitoMatchers
import util.MockAwaitable
import play.api.libs.json._
import play.api.mvc._
import play.api.test._
import play.modules.reactivemongo.ReactiveMongoApi
import repositories.{
  BookingByProjectRepository,
  BookingByTagRepository,
  BookingByTypeRepository
}

import scala.concurrent.{ExecutionContext, Future}

class TimeBookingStatisticsControllerSpec
    extends PlaySpecification
    with Mockito
    with Results
    with MockitoMatchers
    with TestApplication
    with EmbedMongo {

  "getAggregatedStatistics" should {
    "empty sequence if no bookingstatistics where found" in new WithTestApplication {
      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: TimeBookingStatisticsController
        with SecurityControllerMock
        with MockCacheAware =
        TimeBookingStatisticsControllerMock(systemServices,
                                            authConfig,
                                            reactiveMongoApi)

      controller.bookingByTagRepository
        .findAggregatedByUserAndRange(any[EntityReference[UserId]],
                                      any[OrganisationId],
                                      any[LocalDate],
                                      any[LocalDate],
                                      anyString,
                                      any[Granularity])(any[DBSession])
        .returns(Future.successful(List()))

      val to: LocalDate          = LocalDate.now()
      val from: LocalDate        = to.minusDays(1)
      val request: Request[Unit] = FakeRequest().withBody("")
      val result: Future[Result] =
        controller.getAggregatedStatisticsByUser(controller.organisationId,
                                                 "tag",
                                                 from,
                                                 to,
                                                 Day)(request)

      status(result) must equalTo(OK)
      contentAsJson(result) must equalTo(Json.arr())
    }

    "bad request if source was not found" in new WithTestApplication {
      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: TimeBookingStatisticsController
        with SecurityControllerMock
        with MockCacheAware =
        TimeBookingStatisticsControllerMock(systemServices,
                                            authConfig,
                                            reactiveMongoApi)

      val to: LocalDate          = LocalDate.now()
      val from: LocalDate        = to.minusDays(1)
      val request: Request[Unit] = FakeRequest().withBody("")
      val result: Future[Result] =
        controller.getAggregatedStatisticsByUser(controller.organisationId,
                                                 "anysource",
                                                 from,
                                                 to,
                                                 Day)(request)

      status(result) must equalTo(BAD_REQUEST)
    }
  }
}

object TimeBookingStatisticsControllerMock extends MockAwaitable with Mockito {
  def apply(systemServices: SystemServices,
            authConfig: AuthConfig,
            reactiveMongoApi: ReactiveMongoApi)(implicit
      ec: ExecutionContext): TimeBookingStatisticsController
    with SecurityControllerMock
    with MockCacheAware = {
    val bookingByProjectRepository = mockAwaitable[BookingByProjectRepository]
    val bookingByTagRepository     = mockAwaitable[BookingByTagRepository]
    val bookingByTypeRepository    = mockAwaitable[BookingByTypeRepository]

    new TimeBookingStatisticsController(
      Helpers.stubControllerComponents(),
      systemServices,
      authConfig,
      MockCache,
      reactiveMongoApi,
      bookingByProjectRepository,
      bookingByTagRepository,
      bookingByTypeRepository) with SecurityControllerMock with MockCacheAware
  }
}
