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
  MockCache,
  MockCacheAware,
  MockServices,
  SystemServices,
  TestApplication
}
import models._
import mongo.EmbedMongo
import org.joda.time.DateTime
import org.specs2.mock.Mockito
import org.specs2.mock.mockito.MockitoMatchers
import play.api.mvc._
import play.api.test._
import play.modules.reactivemongo.ReactiveMongoApi
import util.MockAwaitable

import scala.concurrent.{ExecutionContext, Future}

class TimeBookingControllerSpec
    extends PlaySpecification
    with Mockito
    with Results
    with MockitoMatchers
    with TestApplication
    with EmbedMongo {

  "start booking" should {

    "unauthorized if for authenticated user project id does not exist" in new WithTestApplication {
      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices                              = inject[SystemServices]
      val authConfig                                  = inject[AuthConfig]

      val controller: TimeBookingController
        with SecurityControllerMock
        with MockCacheAware =
        TimeBookingControllerMock(systemServices, authConfig, reactiveMongoApi)
      val projectId: ProjectId = ProjectId()
      val request: FakeRequest[StartBookingRequest] = FakeRequest()
        .withBody(
          StartBookingRequest(
            projectId = projectId,
            tags = Set(SimpleTag(TagId("tag1"))),
            start = None
          ))
      val result: Future[Result] =
        controller.start(controller.organisationId)(request)

      status(result) must equalTo(UNAUTHORIZED)
    }

    "badrequest if for authenticated user organisation does not exist" in new WithTestApplication {
      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices                              = inject[SystemServices]
      val authConfig                                  = inject[AuthConfig]
      val controller: TimeBookingController
        with SecurityControllerMock
        with MockCacheAware =
        TimeBookingControllerMock(systemServices, authConfig, reactiveMongoApi)

      val request: FakeRequest[StartBookingRequest] = FakeRequest()
        .withBody(
          StartBookingRequest(
            projectId = controller.project.id,
            tags = Set(SimpleTag(TagId("tag1"))),
            start = None
          ))
      val organisationId: OrganisationId = OrganisationId()
      val result: Future[Result] = controller.start(organisationId)(request)

      status(result) must equalTo(UNAUTHORIZED)
    }
  }

  "edit booking" should {

    "unauthorized if for authenticated user project id does not exist" in new WithTestApplication {
      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices                              = inject[SystemServices]
      val authConfig                                  = inject[AuthConfig]
      val controller: TimeBookingController
        with SecurityControllerMock
        with MockCacheAware =
        TimeBookingControllerMock(systemServices, authConfig, reactiveMongoApi)

      val projectId: ProjectId = ProjectId()
      val request: FakeRequest[EditBookingRequest] = FakeRequest()
        .withBody(
          EditBookingRequest(
            projectId = Some(projectId),
            tags = None,
            start = None,
            end = None
          ))
      val result: Future[Result] =
        controller.edit(controller.organisationId, BookingId())(request)

      status(result) must equalTo(UNAUTHORIZED)
    }

    "unauthorized if for authenticated user organisation does not exist" in new WithTestApplication {
      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices                              = inject[SystemServices]
      val authConfig                                  = inject[AuthConfig]
      val controller: TimeBookingController
        with SecurityControllerMock
        with MockCacheAware =
        TimeBookingControllerMock(systemServices, authConfig, reactiveMongoApi)

      val request: FakeRequest[EditBookingRequest] = FakeRequest()
        .withBody(
          EditBookingRequest(
            projectId = Some(controller.project.id),
            tags = None,
            start = None,
            end = None
          ))
      val organisationId: OrganisationId = OrganisationId()
      val result: Future[Result] =
        controller.edit(organisationId, BookingId())(request)

      status(result) must equalTo(UNAUTHORIZED)
    }

    "badrequest if end date is before start date" in new WithTestApplication {
      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices                              = inject[SystemServices]
      val authConfig                                  = inject[AuthConfig]
      val controller: TimeBookingController
        with SecurityControllerMock
        with MockCacheAware =
        TimeBookingControllerMock(systemServices, authConfig, reactiveMongoApi)

      val from: DateTime = DateTime.now()
      val to: DateTime   = from.minusDays(1)
      val request: FakeRequest[EditBookingRequest] = FakeRequest()
        .withBody(
          EditBookingRequest(
            projectId = None,
            tags = None,
            start = Some(from),
            end = Some(Some(to))
          ))
      val result: Future[Result] =
        controller.edit(controller.organisationId, BookingId())(request)

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must startWith(
        "Start date needs to be before end date")
    }
  }

  "add booking" should {

    "Unauthorized if for authenticated user project id does not exist" in new WithTestApplication {
      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices                              = inject[SystemServices]
      val authConfig                                  = inject[AuthConfig]
      val controller: TimeBookingController
        with SecurityControllerMock
        with MockCacheAware =
        TimeBookingControllerMock(systemServices, authConfig, reactiveMongoApi)

      val from: DateTime       = DateTime.now()
      val to: DateTime         = from.plusHours(1)
      val projectId: ProjectId = ProjectId()
      val request: FakeRequest[AddBookingRequest] = FakeRequest()
        .withBody(
          models.AddBookingRequest(projectId = projectId,
                                   tags = Set(SimpleTag(TagId("tag1"))),
                                   start = from,
                                   end = to))
      val result: Future[Result] =
        controller.add(controller.organisationId)(request)

      status(result) must equalTo(UNAUTHORIZED)
    }

    "Unauthorized if for authenticated user organisation does not exist" in new WithTestApplication {
      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices                              = inject[SystemServices]
      val authConfig                                  = inject[AuthConfig]
      val controller: TimeBookingController
        with SecurityControllerMock
        with MockCacheAware =
        TimeBookingControllerMock(systemServices, authConfig, reactiveMongoApi)

      val from: DateTime = DateTime.now()
      val to: DateTime   = from.plusHours(1)
      val request: FakeRequest[AddBookingRequest] = FakeRequest()
        .withBody(
          models.AddBookingRequest(projectId = controller.project.id,
                                   tags = Set(SimpleTag(TagId("tag1"))),
                                   start = from,
                                   end = to))
      val organisationId: OrganisationId = OrganisationId()
      val result: Future[Result] = controller.add(organisationId)(request)

      status(result) must equalTo(UNAUTHORIZED)
    }

    "badrequest if end date is before start date" in new WithTestApplication {
      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices                              = inject[SystemServices]
      val authConfig                                  = inject[AuthConfig]
      val controller: TimeBookingController
        with SecurityControllerMock
        with MockCacheAware =
        TimeBookingControllerMock(systemServices, authConfig, reactiveMongoApi)

      val from: DateTime = DateTime.now()
      val to: DateTime   = from.minusHours(1)
      val request: FakeRequest[AddBookingRequest] = FakeRequest()
        .withBody(
          models.AddBookingRequest(projectId = controller.project.id,
                                   tags = Set(SimpleTag(TagId("tag1"))),
                                   start = from,
                                   end = to))
      val result: Future[Result] =
        controller.add(controller.organisationId)(request)

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must startWith(
        "Start date needs to be before end date")
    }
  }
}

object TimeBookingControllerMock extends MockAwaitable with Mockito {
  def apply(systemServices: SystemServices,
            authConfig: AuthConfig,
            reactiveMongoApi: ReactiveMongoApi)(implicit
      ec: ExecutionContext): TimeBookingController
    with SecurityControllerMock
    with MockCacheAware = {

    new TimeBookingController(
      Helpers.stubControllerComponents(),
      authConfig,
      MockCache,
      reactiveMongoApi,
      systemServices) with SecurityControllerMock with MockCacheAware
  }
}
