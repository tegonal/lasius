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

import core.Validation.ValidationFailedException
import core.{DBSession, SystemServices}
import models._
import org.joda.time.DateTime
import play.api.cache.AsyncCacheApi
import play.api.libs.json.Json
import play.api.mvc.{Action, ControllerComponents}
import play.modules.reactivemongo.ReactiveMongoApi
import repositories.{
  InvitationRepository,
  OrganisationRepository,
  ProjectRepository,
  UserRepository
}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class InvitationsController @Inject() (
    controllerComponents: ControllerComponents,
    override val systemServices: SystemServices,
    userRepository: UserRepository,
    organisationRepository: OrganisationRepository,
    invitationRepository: InvitationRepository,
    projectRepository: ProjectRepository,
    override val authConfig: AuthConfig,
    override val cache: AsyncCacheApi,
    override val reactiveMongoApi: ReactiveMongoApi)(implicit
    ec: ExecutionContext)
    extends BaseLasiusController(controllerComponents) {

  /** Unauthenticated endpoint
    */
  def getStatus(invitationId: InvitationId): Action[Unit] = {
    Action.async(parse.empty) { _ =>
      checked {
        withDBSession() { implicit dbSession =>
          for {
            invitation <- validateInvitation(invitationId)
            _ <- validate(invitation.expiration.isAfter(DateTime.now()),
                          "invitation_expired")
            _ <- validate(invitation.outcome.isEmpty,
                          "invitation_invalid_state")
            user <- userRepository.findByEmail(invitation.invitedEmail)
            _    <- validate(user.fold(true)(_.active), "user_deactivated")
          } yield {
            val status =
              user.fold[InvitationStatusResponse](
                InvitationStatusResponse(UnregisteredUser, invitation))(_ =>
                InvitationStatusResponse(InvitationOk, invitation))
            Ok(Json.toJson(status))
          }
        }
      }
    }
  }

  /** Unauthenticated endpoint
    */
  def registerUser(invitationId: InvitationId): Action[UserRegistration] = {
    Action.async(validateJson[UserRegistration]) { request =>
      checked {
        withinTransaction { implicit dbSession =>
          for {
            invitation <- validateInvitation(invitationId)
            _          <- validateNonBlankString("key", request.body.key)
            // first validate user before applying any changes to support
            // setup without real transactions
            _ <- userRepository.validateCreate(invitation.invitedEmail,
                                               request.body)
            // Create new private organisation
            newOrg <- organisationRepository.create(
              key = request.body.key,
              `private` = true,
              settings = None)(systemServices.systemSubject, dbSession)
            // Create new user and assign to private organisation
            _ <- userRepository.create(invitation.invitedEmail,
                                       request.body,
                                       newOrg,
                                       OrganisationAdministrator)
          } yield Ok(Json.toJson(invitation))
        }
      }
    }
  }

  def getDetails(invitationId: InvitationId): Action[Unit] = {
    HasUserRole(FreeUser, parse.empty, withinTransaction = false) {
      implicit dbSession => implicit subject => implicit user =>
        implicit request =>
          for {
            invitation <- validateInvitationAndUser(invitationId)
          } yield Ok(Json.toJson(invitation))
    }
  }

  def accept(invitationId: InvitationId): Action[AcceptInvitationRequest] = {
    HasUserRole(FreeUser,
                validateJson[AcceptInvitationRequest],
                withinTransaction = true) {
      implicit dbSession => implicit subject => implicit user =>
        implicit request =>
          HasOptionalOrganisationRole(
            user,
            request.body.organisationReference.map(_.id),
            OrganisationMember) { maybeUserOrg =>
            for {
              invitation <- validateInvitationAndUser(invitationId)

              _ <- invitation match {
                case i: JoinProjectInvitation =>
                  for {
                    // needs to have specified an organisation to which we want to assign the project
                    userOrg <- maybeUserOrg.fold[Future[UserOrganisation]](
                      Future.failed(ValidationFailedException(
                        "Need to specify binding organisation when joining a project")))(
                      Future.successful(_))
                    project <- projectRepository
                      .findById(i.projectReference.id)
                      .noneToFailed(
                        s"Project ${i.projectReference.key} does not exist")
                    _ <- validate(
                      project.active,
                      s"Cannot join inactive project ${i.projectReference.key}")
                    result <- userRepository.assignUserToProject(
                      user.id,
                      userOrg.organisationReference,
                      i.projectReference,
                      i.role)
                  } yield result
                case i: JoinOrganisationInvitation =>
                  for {
                    org <- organisationRepository
                      .findById(i.organisationReference.id)
                      .noneToFailed(
                        s"Organisation ${i.organisationReference.key} does not exist")
                    _ <- validate(
                      org.active,
                      s"Cannot join inactive organisation ${i.organisationReference.key}")
                    result <- userRepository.assignUserToOrganisation(
                      user.id,
                      org,
                      i.role,
                      WorkingHours())
                  } yield result

              }
              result <- invitationRepository.updateInvitationStatus(
                invitationId,
                InvitationAccepted)
              _                 <- validate(result, "failed_update_status")
              updatedInvitation <- invitationRepository.findById(invitationId)

            } yield Ok(Json.toJson(updatedInvitation))
          }
    }
  }

  def decline(invitationId: InvitationId): Action[Unit] = {
    HasUserRole(FreeUser, parse.empty, withinTransaction = true) {
      implicit dbSession => implicit subject => implicit user =>
        implicit request =>
          for {
            _ <- validateInvitationAndUser(invitationId)
            result <- invitationRepository.updateInvitationStatus(
              invitationId,
              InvitationDeclined)
            _          <- validate(result, "failed_update_status")
            invitation <- invitationRepository.findById(invitationId)
          } yield Ok(Json.toJson(invitation))
    }
  }

  private def validateInvitationAndUser(invitationId: InvitationId)(implicit
      dbSession: DBSession,
      user: User): Future[Invitation] = {
    for {
      invitation <- validateInvitation(invitationId)
      _ <- validate(user.email == invitation.invitedEmail, s"illegal_access")
    } yield invitation
  }

  private def validateInvitation(invitationId: InvitationId)(implicit
      dbSession: DBSession): Future[Invitation] = {
    invitationRepository
      .findById(invitationId)
      .noneToFailed(s"invitation_not_found")
  }
}
