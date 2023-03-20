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
import scala.concurrent.ExecutionContext

class InvitationsControllerMock(
    controllerComponents: ControllerComponents,
    systemServices: SystemServices,
    val organisationRepository: OrganisationMongoRepository,
    userMongoRepository: UserMongoRepository,
    val invitationRepository: InvitationMongoRepository,
    val projectRepository: ProjectMongoRepository,
    authConfig: AuthConfig,
    cache: AsyncCacheApi,
    reactiveMongoApi: ReactiveMongoApi,
    override val userActive: Boolean)(implicit ec: ExecutionContext)
    extends InvitationsController(controllerComponents,
                                  systemServices,
                                  userMongoRepository,
                                  organisationRepository,
                                  invitationRepository,
                                  projectRepository,
                                  authConfig,
                                  cache,
                                  reactiveMongoApi)
    with SecurityControllerMock
    with MockCacheAware
    with TestDBSupport {

  // override mock as we deal with a real db backend in this spec
  override val userRepository: UserRepository = userMongoRepository
}

object InvitationsControllerMock
    extends MockAwaitable
    with Mockito
    with Awaitable {
  def apply(systemServices: SystemServices,
            authConfig: AuthConfig,
            reactiveMongoApi: ReactiveMongoApi,
            userActive: Boolean = true)(implicit
      ec: ExecutionContext): InvitationsControllerMock = {
    val userMongoRepository         = new UserMongoRepository()
    val invitationMongoRepository   = new InvitationMongoRepository()
    val organisationMongoRepository = new OrganisationMongoRepository()
    val projectMongoRepository      = new ProjectMongoRepository()

    val controller = new InvitationsControllerMock(
      Helpers.stubControllerComponents(),
      systemServices,
      organisationMongoRepository,
      userMongoRepository,
      invitationMongoRepository,
      projectMongoRepository,
      authConfig,
      MockCache,
      reactiveMongoApi,
      userActive)

    // initialize data
    controller
      .withDBSession() { implicit dbSession =>
        for {
          // drop previous data
          _ <- userMongoRepository.dropAll()
          _ <- organisationMongoRepository.dropAll()
          _ <- projectMongoRepository.dropAll()

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
