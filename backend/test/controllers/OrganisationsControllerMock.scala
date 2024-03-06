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

import core.{MockCache, MockCacheAware, SystemServices, TestDBSupport}
import models.{OrganisationAdministrator, OrganisationRole}
import org.specs2.mock.Mockito
import play.api.cache.AsyncCacheApi
import play.api.mvc.ControllerComponents
import play.api.test.Helpers
import play.modules.reactivemongo.ReactiveMongoApi
import repositories._
import util.{Awaitable, MockAwaitable}

import scala.concurrent.ExecutionContext

class OrganisationsControllerMock(
    controllerComponents: ControllerComponents,
    systemServices: SystemServices,
    val organisationRepository: OrganisationMongoRepository,
    userMongoRepository: UserMongoRepository,
    val invitationRepository: InvitationMongoRepository,
    val projectRepository: ProjectMongoRepository,
    val publicHolidayRepository: PublicHolidayRepository,
    authConfig: AuthConfig,
    cache: AsyncCacheApi,
    reactiveMongoApi: ReactiveMongoApi,
    override val organisationRole: OrganisationRole,
    override val isOrganisationPrivate: Boolean,
    override val organisationActive: Boolean)(implicit ec: ExecutionContext)
    extends OrganisationsController(controllerComponents,
                                    systemServices,
                                    organisationRepository,
                                    userMongoRepository,
                                    invitationRepository,
                                    projectRepository,
                                    publicHolidayRepository,
                                    authConfig,
                                    cache,
                                    reactiveMongoApi)
    with SecurityControllerMock
    with MockCacheAware
    with TestDBSupport {

  // override mock as we deal with a real db backend in this spec
  override val userRepository: UserRepository = userMongoRepository
}

object OrganisationsControllerMock
    extends MockAwaitable
    with Mockito
    with Awaitable {
  def apply(systemServices: SystemServices,
            authConfig: AuthConfig,
            reactiveMongoApi: ReactiveMongoApi,
            organisationRole: OrganisationRole = OrganisationAdministrator,
            isOrganisationPrivate: Boolean = false,
            organisationActive: Boolean = true)(implicit
      ec: ExecutionContext): OrganisationsControllerMock = {
    val userMongoRepository          = new UserMongoRepository()
    val projectMongoRepository       = new ProjectMongoRepository()
    val invitationMongoRepository    = new InvitationMongoRepository()
    val organisationMongoRepository  = new OrganisationMongoRepository()
    val publicHolidayMongoRepository = new PublicHolidayMongoRepository()

    val controller = new OrganisationsControllerMock(
      Helpers.stubControllerComponents(),
      systemServices,
      organisationMongoRepository,
      userMongoRepository,
      invitationMongoRepository,
      projectMongoRepository,
      publicHolidayMongoRepository,
      authConfig,
      MockCache,
      reactiveMongoApi,
      organisationRole,
      isOrganisationPrivate = isOrganisationPrivate,
      organisationActive = organisationActive)

    // initialize data
    controller
      .withDBSession() { implicit dbSession =>
        for {
          // drop previous data
          _ <- userMongoRepository.dropAll()
          _ <- projectMongoRepository.dropAll()
          _ <- organisationMongoRepository.dropAll()

          // initialize user
          _ <- userMongoRepository.upsert(controller.user)
          _ <- projectMongoRepository.upsert(controller.project)
          _ <- organisationMongoRepository.upsert(controller.organisation)
        } yield ()
      }
      .awaitResult()

    controller
  }
}
