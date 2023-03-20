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

import actors.TagCache.{CachedTags, GetTags}
import akka.pattern.ask
import akka.util.Timeout
import core.SystemServices
import models._
import play.api.cache.AsyncCacheApi
import play.api.libs.json.Json
import play.api.mvc.{Action, ControllerComponents}
import play.modules.reactivemongo.ReactiveMongoApi
import repositories._

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps

class TagController @Inject() (controllerComponents: ControllerComponents,
                               override val systemServices: SystemServices,
                               override val authConfig: AuthConfig,
                               override val cache: AsyncCacheApi,
                               override val userRepository: UserRepository,
                               override val reactiveMongoApi: ReactiveMongoApi,
                               projectRepository: ProjectRepository)(implicit
    ec: ExecutionContext)
    extends BaseLasiusController(controllerComponents)
    with SecurityRepositoryComponent {

  def getTags(orgId: OrganisationId, projectId: ProjectId): Action[Unit] = {
    HasUserRole(FreeUser, parse.empty, withinTransaction = false) {
      implicit dbSession => implicit subject => user => implicit request =>
        HasOrganisationRole(user, orgId, OrganisationMember) { userOrg =>
          HasProjectRole(userOrg, projectId, ProjectMember) { userProject =>
            implicit val timeout: Timeout =
              Timeout(5 seconds) // needed for `?` below
            val future = systemServices.tagCache ? GetTags(projectId)
            for {
              cachedTags <- future.map { result =>
                val tagResult = result.asInstanceOf[CachedTags]
                tagResult.tags
              }
              project <- projectRepository.findById(projectId)

            } yield {
              val projectTags =
                project.map(_.bookingCategories).getOrElse(Set())
              val allTags = (cachedTags ++ projectTags).toSeq.sortBy(_.id.value)
              Ok(Json.toJson(allTags))
            }
          }
        }
    }
  }
}
