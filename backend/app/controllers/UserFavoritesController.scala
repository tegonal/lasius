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
import akka.util.Timeout
import core.SystemServices
import models._
import play.api.cache.AsyncCacheApi
import play.api.libs.json._
import play.api.mvc.{Action, ControllerComponents}
import play.modules.reactivemongo.ReactiveMongoApi
import repositories._

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class UserFavoritesController @Inject() (
                                          controllerComponents: ControllerComponents,
                                          override val systemServices: SystemServices,
                                          override val authConfig: AuthConfig,
                                          override val authTokenCache: AsyncCacheApi,
                                          override val reactiveMongoApi: ReactiveMongoApi,
                                          userFavoritesRepository: UserFavoritesRepository,
                                          clientReceiver: ClientReceiver)(implicit ec: ExecutionContext)
    extends BaseLasiusController(controllerComponents) {

  implicit val timeout: Timeout = systemServices.timeout

  def getFavorites(orgId: OrganisationId): Action[Unit] =
    HasUserRole(FreeUser, parse.empty, withinTransaction = false) {
      implicit dbSession => implicit subject => user => implicit request =>
        HasOrganisationRole(user, orgId, OrganisationMember) { _ =>
          userFavoritesRepository.getByUser(subject.userReference, orgId).map {
            favorites =>
              Ok(Json.toJson(favorites))
          }
        }
    }

  def addFavorite(orgId: OrganisationId): Action[FavoritesRequest] =
    HasUserRole(FreeUser,
                validateJson[FavoritesRequest],
                withinTransaction = false) {
      implicit dbSession => implicit subject => user => implicit request =>
        HasOrganisationRole(user, orgId, OrganisationMember) { userOrg =>
          HasProjectRole(userOrg, request.body.projectId, ProjectMember) {
            userProject =>
              for {
                favorites <- userFavoritesRepository
                  .addFavorite(subject.userReference,
                               orgId,
                               userProject.projectReference,
                               request.body.tags)
              } yield {
                clientReceiver ! (subject.userReference.id, FavoriteAdded(
                  subject.userReference.id,
                  orgId,
                  BookingStub(userProject.projectReference,
                              request.body.tags)), List(
                  subject.userReference.id))
                Ok(Json.toJson(favorites))
              }
          }
        }
    }

  def removeFavorite(orgId: OrganisationId): Action[FavoritesRequest] =
    HasUserRole(FreeUser,
                validateJson[FavoritesRequest],
                withinTransaction = false) {
      implicit dbSession => implicit subject => user => implicit request =>
        HasOrganisationRole(user, orgId, OrganisationMember) { userOrg =>
          HasProjectRole(userOrg, request.body.projectId, ProjectMember) {
            userProject =>
              for {
                favorites <- userFavoritesRepository
                  .removeFavorite(subject.userReference,
                                  orgId,
                                  BookingStub(userProject.projectReference,
                                              request.body.tags))
              } yield {
                clientReceiver ! (subject.userReference.id, FavoriteRemoved(
                  subject.userReference.id,
                  orgId,
                  BookingStub(userProject.projectReference,
                              request.body.tags)), List(
                  subject.userReference.id))
                Ok(Json.toJson(favorites))
              }
          }
        }
    }
}
