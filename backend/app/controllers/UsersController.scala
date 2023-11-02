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

import akka.util.Timeout
import core.SystemServices
import models._
import org.pac4j.core.context.session.SessionStore
import org.pac4j.play.scala.SecurityComponents
import play.api.libs.json._
import play.api.mvc._
import play.modules.reactivemongo.ReactiveMongoApi
import repositories.UserRepository

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class UsersController @Inject() (
    override val controllerComponents: SecurityComponents,
    override val systemServices: SystemServices,
    override val authConfig: AuthConfig,
    override val reactiveMongoApi: ReactiveMongoApi,
    override val playSessionStore: SessionStore,
    userRepository: UserRepository)(implicit ec: ExecutionContext)
    extends BaseLasiusController(controllerComponents) {

  implicit val timeout: Timeout = systemServices.timeout

  /** Retrieves a logged in user if the authentication token is valid.
    *
    * If the token is invalid, [[HasToken]] does not invoke this function.
    *
    * returns The user in JSON format.
    */
  def authUser(): Action[Unit] =
    HasToken(parse.empty, withinTransaction = false) {
      implicit dbSession => subject => implicit request =>
        {
          userRepository
            .findByUserReference(subject.userReference)
            .map(_.map(user => Ok(Json.toJson(user.toDTO())))
              .getOrElse(BadRequest))
        }
    }

  def updateUserSettings(): Action[UserSettings] =
    HasUserRole(FreeUser,
                validateJson[UserSettings],
                withinTransaction = false) {
      implicit dbSession => implicit subject => _ => implicit request =>
        {
          userRepository
            .updateUserSettings(subject.userReference, request.body)
            .map(user => Ok(Json.toJson(user.toDTO())))
        }
    }

  def updatePersonalData(): Action[PersonalDataUpdate] =
    HasUserRole(FreeUser,
                validateJson[PersonalDataUpdate],
                withinTransaction = false) {
      implicit dbSession => implicit subject => _ => implicit request =>
        {
          userRepository
            .updateUserData(subject.userReference, request.body)
            .map(user => Ok(Json.toJson(user.toDTO())))
        }
    }

  def updateUserData(orgId: OrganisationId,
                     userId: UserId): Action[PersonalDataUpdate] =
    HasUserRole(FreeUser,
                validateJson[PersonalDataUpdate],
                withinTransaction = false) {
      implicit dbSession => implicit subject => user => implicit request =>
        HasOrganisationRole(user, orgId, OrganisationAdministrator) { _ =>
          for {
            user <- userRepository
              .findByOrganisationAndUserId(userId, orgId)
              .noneToFailed(
                s"Could not find user with user with id ${userId.value} in organisation ${orgId.value}")
            result <- userRepository
              .updateUserData(user.getReference(), request.body)
              .map(user => Ok(Json.toJson(user.toDTO())))
          } yield result
        }
    }

  def updateMyUserOrganisationData(
      orgId: OrganisationId): Action[UpdateUserOrganisation] =
    HasUserRole(FreeUser,
                validateJson[UpdateUserOrganisation],
                withinTransaction = false) {
      implicit dbSession => implicit subject => user => implicit request =>
        HasOrganisationRole(user, orgId, OrganisationMember) { _ =>
          for {
            organisationReference <- success()
              .map(_ =>
                user.organisations
                  .find(_.organisationReference.id == orgId)
                  .map(_.organisationReference))
              .noneToFailed(
                s"User ${user.key} is not assigned to organisation ${orgId.value}")
            user <- userRepository
              .updateUserOrganisation(subject.userReference,
                                      organisationReference,
                                      request.body)
          } yield Ok(Json.toJson(user.toDTO()))
        }
    }
}
