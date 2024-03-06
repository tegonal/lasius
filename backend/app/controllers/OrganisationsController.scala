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

import core.SystemServices
import models._
import org.joda.time.{DateTime, LocalDate}
import play.api.cache.AsyncCacheApi
import play.api.libs.json.Json
import play.api.mvc.{Action, ControllerComponents}
import play.modules.reactivemongo.ReactiveMongoApi
import repositories._

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class OrganisationsController @Inject() (
    controllerComponents: ControllerComponents,
    override val systemServices: SystemServices,
    organisationRepository: OrganisationRepository,
    userRepository: UserRepository,
    invitationRepository: InvitationRepository,
    projectRepository: ProjectRepository,
    publicHolidayRepository: PublicHolidayRepository,
    override val authConfig: AuthConfig,
    override val cache: AsyncCacheApi,
    override val reactiveMongoApi: ReactiveMongoApi)(implicit
    ec: ExecutionContext)
    extends BaseLasiusController(controllerComponents) {

  def getOrganisation(organisationId: OrganisationId): Action[Unit] =
    HasUserRole(FreeUser, parse.empty, withinTransaction = false) {
      implicit dbSession => implicit subject => user => implicit request =>
        HasOrganisationRole(user, organisationId, OrganisationMember) { _ =>
          organisationRepository
            .findById(organisationId)
            .map(org => Ok(Json.toJson(org)))
        }
    }

  def createOrganisation(): Action[CreateOrganisation] = {
    HasUserRole(FreeUser,
                validateJson[CreateOrganisation],
                withinTransaction = true) {
      implicit dbSession => implicit subject => _ => implicit request =>
        for {
          // create organisation
          organisation <- organisationRepository
            .create(request.body.key, `private` = false, request.body.settings)
          // assign user as project administrator
          _ <- userRepository.assignUserToOrganisation(
            subject.userReference.id,
            organisation,
            OrganisationAdministrator,
            request.body.plannedWorkingHours.getOrElse(WorkingHours()))
        } yield Created(Json.toJson(organisation))
    }
  }

  def updateOrganisation(
      organisationId: OrganisationId): Action[UpdateOrganisation] = {
    HasUserRole(FreeUser,
                validateJson[UpdateOrganisation],
                withinTransaction = true) {
      implicit dbSession => implicit subject => user => implicit request =>
        HasOrganisationRole(user, organisationId, OrganisationAdministrator) {
          userOrg =>
            for {
              // update organisation
              organisation <- organisationRepository
                .update(userOrg.organisationReference, request.body)
              // update key on referenced entities
              _ <- request.body.key.fold(success()) { newKey =>
                for {
                  _ <- userRepository.updateOrganisationKey(organisationId,
                                                            newKey)
                  _ <- projectRepository.updateOrganisationKey(organisationId,
                                                               newKey)
                  _ <- invitationRepository.updateOrganisationKey(
                    organisationId,
                    newKey)
                } yield play.api.mvc.Results.Ok
              }
            } yield Ok(Json.toJson(organisation))
        }
    }
  }

  def deactivateOrganisation(organisationId: OrganisationId): Action[Unit] =
    HasUserRole(FreeUser, parse.empty, withinTransaction = true) {
      implicit dbSession => implicit subject => user => implicit request =>
        HasOrganisationRole(user, organisationId, OrganisationAdministrator) {
          userOrg =>
            for {
              // remove from all users
              _ <- userRepository.unassignAllUsersFromOrganisation(
                organisationId)
              // deactivate organisation
              _ <- organisationRepository.deactivate(
                userOrg.organisationReference)
            } yield Ok("")
        }
    }

  def getUsers(organisationId: OrganisationId): Action[Unit] =
    HasUserRole(FreeUser, parse.empty, withinTransaction = false) {
      implicit dbSession => implicit subject => user => implicit request =>
        HasOrganisationRole(user, organisationId, OrganisationMember) {
          userOrg =>
            userRepository
              .findByOrganisation(userOrg.organisationReference)
              .map(users => Ok(Json.toJson(users.map(_.stub))))
        }
    }

  def inviteUser(orgId: OrganisationId): Action[UserToOrganisationAssignment] =
    HasUserRole(FreeUser,
                validateJson[UserToOrganisationAssignment],
                withinTransaction = true) {
      implicit dbSession => implicit subject => user => implicit request =>
        HasOrganisationRole(user, orgId, OrganisationAdministrator) { userOrg =>
          for {
            _ <- validateEmail(request.body.email)
            organisation <- organisationRepository
              .findById(orgId)
              .noneToFailed(
                s"Organisation ${userOrg.organisationReference.key} does not exist")
            _ <- validate(
              organisation.active,
              s"Cannot invite to an inactive organisation ${userOrg.organisationReference.key}")
            // create invitation
            invitationId = InvitationId()
            _ <- invitationRepository.upsert(
              JoinOrganisationInvitation(
                id = invitationId,
                invitedEmail = request.body.email,
                createDate = DateTime.now(),
                createdBy = subject.userReference,
                expiration = DateTime.now().plusDays(7),
                organisationReference = userOrg.organisationReference,
                role = request.body.role,
                outcome = None
              ))
          } yield Created(
            Json.toJson(
              InvitationResult(Some(invitationId), request.body.email)))
        }
    }

  def unassignUser(orgId: OrganisationId, userId: UserId): Action[Unit] =
    HasUserRole(FreeUser, parse.empty, withinTransaction = false) {
      implicit dbSession => implicit subject => user => implicit request =>
        HasOrganisationRole(user, orgId, OrganisationAdministrator) { userOrg =>
          for {
            org <- organisationRepository
              .findById(orgId)
              .noneToFailed(
                s"Cannot remove user from non-existing organisation ${userOrg.organisationReference.key}")
            _ <- validate(!org.`private` || org.createdBy.id != userId,
                          s"Cannot remove user from own private organisation")

            _ <- userRepository.unassignUserFromOrganisation(
              userId,
              userOrg.organisationReference)
          } yield Ok("")
        }
    }

  def unassignMyUser(orgId: OrganisationId): Action[Unit] =
    HasUserRole(FreeUser, parse.empty, withinTransaction = false) {
      implicit dbSession => implicit subject => user => implicit request =>
        HasOrganisationRole(user, orgId, OrganisationMember) { userOrg =>
          for {
            org <- organisationRepository
              .findById(orgId)
              .noneToFailed(
                s"Cannot remove user from non-existing organisation ${userOrg.organisationReference.key}")
            _ <- validate(
              !org.`private` || org.createdBy.id != subject.userReference.id,
              s"Cannot remove user from own private organisation")
            _ <- userRepository.unassignUserFromOrganisation(
              subject.userReference.id,
              userOrg.organisationReference)
          } yield Ok("")
        }
    }

  def getPublicHolidays(orgId: OrganisationId,
                        year: Option[Int]): Action[Unit] =
    HasUserRole(FreeUser, parse.empty, withinTransaction = false) {
      implicit dbSession => implicit subject => user => implicit request =>
        HasOrganisationRole(user, orgId, OrganisationMember) { userOrg =>
          for {
            publicHolidays <- publicHolidayRepository.findByOrganisationAndYear(
              userOrg.organisationReference,
              year.getOrElse(LocalDate.now().getYear))
          } yield Ok(Json.toJson(publicHolidays))
        }
    }

  def updatePublicHoliday(orgId: OrganisationId,
                          id: PublicHolidayId): Action[UpdatePublicHoliday] =
    HasUserRole(FreeUser,
                validateJson[UpdatePublicHoliday],
                withinTransaction = false) {
      implicit dbSession => implicit subject => user => implicit request =>
        HasOrganisationRole(user, orgId, OrganisationMember) { userOrg =>
          for {
            publicHoliday <- publicHolidayRepository.update(
              userOrg.organisationReference,
              id,
              request.body)
          } yield Ok(Json.toJson(publicHoliday))
        }
    }

  def createPublicHoliday(orgId: OrganisationId): Action[CreatePublicHoliday] =
    HasUserRole(FreeUser,
                validateJson[CreatePublicHoliday],
                withinTransaction = false) {
      implicit dbSession => implicit subject => user => implicit request =>
        HasOrganisationRole(user, orgId, OrganisationMember) { userOrg =>
          for {
            publicHoliday <- publicHolidayRepository.create(
              userOrg.organisationReference,
              request.body)
          } yield Created(Json.toJson(publicHoliday))
        }
    }

  def deletePublicHoliday(orgId: OrganisationId,
                          id: PublicHolidayId): Action[Unit] =
    HasUserRole(FreeUser, parse.empty, withinTransaction = false) {
      implicit dbSession => implicit subject => user => implicit request =>
        HasOrganisationRole(user, orgId, OrganisationMember) { _ =>
          for {
            _ <- publicHolidayRepository.removeById(id)
          } yield NoContent
        }
    }
}
