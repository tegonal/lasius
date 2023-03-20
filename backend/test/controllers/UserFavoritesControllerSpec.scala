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

import actors.ClientReceiver
import controllers.TimeBookingStatisticsControllerMock.mock
import core.{MockCache, MockCacheAware, SystemServices, TestApplication}
import models._
import mongo.EmbedMongo
import org.specs2.mock.Mockito
import org.specs2.mock.mockito.MockitoMatchers
import play.api.mvc._
import play.api.test._
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.bson.BSONObjectID
import repositories.UserFavoritesRepository
import util.MockAwaitable

import scala.concurrent.{ExecutionContext, Future}

class UserFavoritesControllerSpec
    extends PlaySpecification
    with Mockito
    with Results
    with MockitoMatchers
    with TestApplication
    with EmbedMongo {

  "add favorite" should {
    "Unauthorized if for authenticated user project id does not exist" in new WithTestApplication {
      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val userFavoritesRepository = inject[UserFavoritesRepository]

      val controller: UserFavoritesController
        with SecurityControllerMock
        with MockCacheAware =
        UserFavoritesControllerMock(systemServices,
                                    reactiveMongoApi,
                                    authConfig,
                                    userFavoritesRepository)

      val projectId: ProjectId = ProjectId()
      val request: FakeRequest[FavoritesRequest] = FakeRequest()
        .withBody(
          FavoritesRequest(
            projectId = projectId,
            tags = Set(SimpleTag(TagId("tag1")))
          ))
      val result: Future[Result] =
        controller.addFavorite(controller.organisationId)(request)

      status(result) must equalTo(UNAUTHORIZED)
    }

    "Unauthorized if for authenticated user team does not exist" in new WithTestApplication {
      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val userFavoritesRepository = inject[UserFavoritesRepository]

      val controller: UserFavoritesController
        with SecurityControllerMock
        with MockCacheAware =
        UserFavoritesControllerMock(systemServices,
                                    reactiveMongoApi,
                                    authConfig,
                                    userFavoritesRepository)

      val request: FakeRequest[FavoritesRequest] = FakeRequest()
        .withBody(
          FavoritesRequest(
            projectId = controller.project.id,
            tags = Set(SimpleTag(TagId("tag1")))
          ))
      val organisationId: OrganisationId = OrganisationId()
      val result: Future[Result] =
        controller.addFavorite(organisationId)(request)

      status(result) must equalTo(UNAUTHORIZED)
    }
  }

  "remove favorite" should {
    "Unauthorized if for authenticated user project id does not exist" in new WithTestApplication
      with Injecting {
      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val userFavoritesRepository = inject[UserFavoritesRepository]

      val controller: UserFavoritesController
        with SecurityControllerMock
        with MockCacheAware =
        UserFavoritesControllerMock(systemServices,
                                    reactiveMongoApi,
                                    authConfig,
                                    userFavoritesRepository)

      val projectId: ProjectId = ProjectId()
      val request: FakeRequest[FavoritesRequest] = FakeRequest()
        .withBody(
          FavoritesRequest(
            projectId = projectId,
            tags = Set(SimpleTag(TagId("tag1")))
          ))
      val result: Future[Result] =
        controller.removeFavorite(controller.organisationId)(request)

      status(result) must equalTo(UNAUTHORIZED)
    }
  }

  "Unauthorized if for authenticated user team does not exist" in new WithTestApplication
    with Injecting {
    implicit val executionContext: ExecutionContext = inject[ExecutionContext]
    val systemServices: SystemServices              = inject[SystemServices]
    val authConfig: AuthConfig                      = inject[AuthConfig]
    val userFavoritesRepository = inject[UserFavoritesRepository]

    val controller: UserFavoritesController
      with SecurityControllerMock
      with MockCacheAware =
      UserFavoritesControllerMock(systemServices,
                                  reactiveMongoApi,
                                  authConfig,
                                  userFavoritesRepository)

    val request: FakeRequest[FavoritesRequest] = FakeRequest()
      .withBody(
        FavoritesRequest(
          projectId = controller.project.id,
          tags = Set(SimpleTag(TagId("tag1")))
        ))
    val organisationId: OrganisationId = OrganisationId()
    val result: Future[Result] =
      controller.removeFavorite(organisationId)(request)

    status(result) must equalTo(UNAUTHORIZED)
  }

}

object UserFavoritesControllerMock extends MockAwaitable with Mockito {
  def apply(systemServices: SystemServices,
            reactiveMongoApi: ReactiveMongoApi,
            authConfig: AuthConfig,
            userFavoritesRepository: UserFavoritesRepository)(implicit
      ec: ExecutionContext): UserFavoritesController
    with SecurityControllerMock
    with MockCacheAware = {
    val clientReceiver = mock[ClientReceiver]

    new UserFavoritesController(
      Helpers.stubControllerComponents(),
      systemServices,
      authConfig,
      MockCache,
      reactiveMongoApi,
      userFavoritesRepository,
      clientReceiver) with SecurityControllerMock with MockCacheAware
  }
}
