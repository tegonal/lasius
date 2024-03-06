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
import domain.UserTimeBookingAggregate._
import models._
import play.api.cache.AsyncCacheApi
import play.api.mvc.{Action, ControllerComponents}
import play.modules.reactivemongo.ReactiveMongoApi
import repositories.BookingHistoryRepository

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TimeBookingController @Inject() (
    controllerComponents: ControllerComponents,
    override val authConfig: AuthConfig,
    override val cache: AsyncCacheApi,
    override val reactiveMongoApi: ReactiveMongoApi,
    override val systemServices: SystemServices,
    val bookingHistoryRepository: BookingHistoryRepository)(implicit
    ec: ExecutionContext)
    extends BaseLasiusController(controllerComponents) {

  override val supportTransaction: Boolean = systemServices.supportTransaction

  implicit val timeout: Timeout = systemServices.timeout

  def stopProjectBooking(
      orgId: OrganisationId,
      bookingId: BookingId): Action[StopProjectBookingRequest] =
    HasUserRole(FreeUser,
                validateJson[StopProjectBookingRequest],
                withinTransaction = false) {
      implicit dbSession => implicit subject => user => implicit request =>
        HasOrganisationRole(user, orgId, OrganisationMember) { userOrg =>
          systemServices.timeBookingViewService ! EndProjectBookingCommand(
            subject.userReference,
            userOrg.organisationReference,
            bookingId,
            request.body.end
          )
          success()
        }
    }

  def removeBooking(orgId: OrganisationId, bookingId: BookingId): Action[Unit] =
    HasUserRole(FreeUser, parse.empty, withinTransaction = false) {
      implicit dbSession => implicit subject => user => implicit request =>
        HasOrganisationRole(user, orgId, OrganisationMember) { userOrg =>
          for {
            _ <- bookingHistoryRepository
              .findByOrganisationAndId(userOrg.organisationReference, bookingId)
              .noneToFailed(
                s"Cannot find booking ${bookingId.value} in organisation ${userOrg.organisationReference.key}")
            _ = systemServices.timeBookingViewService ! RemoveBookingCommand(
              subject.userReference,
              userOrg.organisationReference,
              bookingId)
          } yield Ok
        }
    }

  def updateProjectBooking(
      orgId: OrganisationId,
      bookingId: BookingId): Action[UpdateProjectBookingRequest] =
    HasUserRole(FreeUser,
                validateJson[UpdateProjectBookingRequest],
                withinTransaction = false) {
      implicit dbSession => implicit subject => user => implicit request =>
        HasOrganisationRole(user, orgId, OrganisationMember) { userOrg =>
          HasOptionalProjectRole(userOrg,
                                 request.body.projectId,
                                 ProjectMember) { maybeUserProject =>
            for {
              _ <- request.body.start
                .flatMap(start =>
                  request.body.end.map(end =>
                    validateStartBeforeEnd(start, end)))
                .getOrElse(Future.successful(Ok))
              _ <- bookingHistoryRepository
                .findByOrganisationAndId(userOrg.organisationReference,
                                         bookingId)
                .noneToFailed(
                  s"Cannot find booking ${bookingId.value} in organisation ${userOrg.organisationReference.key}")
              command <- request.body
                .toCommand(bookingId,
                           userOrg.organisationReference,
                           maybeUserProject.map(_.projectReference))
              _ = systemServices.timeBookingViewService ! command
            } yield Ok
          }
        }
    }

  def changeProjectBookingStart(
      orgId: OrganisationId,
      bookingId: BookingId): Action[ProjectBookingChangeStartRequest] =
    HasUserRole(FreeUser,
                validateJson[ProjectBookingChangeStartRequest],
                withinTransaction = false) {
      implicit dbSession => implicit subject => user => implicit request =>
        HasOrganisationRole(user, orgId, OrganisationMember) {
          userOrganisation =>
            systemServices.timeBookingViewService ! ChangeStartTimeOfBooking(
              subject.userReference,
              userOrganisation.organisationReference,
              bookingId,
              request.body.newStart)
            success()
        }
    }

  def startOrAddProjectBooking(
      orgId: OrganisationId): Action[StartOrAddProjectBookingRequest] =
    HasUserRole(FreeUser,
                validateJson[StartOrAddProjectBookingRequest],
                withinTransaction = false) {
      implicit dbSession => implicit subject => user => implicit request =>
        HasOrganisationRole(user, orgId, OrganisationMember) { userOrg =>
          HasProjectRole(userOrg, request.body.projectId, ProjectMember) {
            userProject =>
              for {
                _ <- request.body.end.fold(success())(
                  validateStartBeforeEnd(request.body.start, _))
                command <- request.body
                  .toCommand(userOrg.organisationReference,
                             userProject.projectReference)
                _ = systemServices.timeBookingViewService ! command
              } yield Ok
          }
        }
    }

  def addAbsenceBooking(
      orgId: OrganisationId): Action[AddAbsenceBookingRequest] =
    HasUserRole(FreeUser,
                validateJson[AddAbsenceBookingRequest],
                withinTransaction = false) {
      implicit dbSession => implicit subject => user => implicit request =>
        HasOrganisationRole(user, orgId, OrganisationMember) { userOrg =>
          for {
            _ <- request.body.end.fold(success())(
              validateStartBeforeEnd(request.body.start, _))
            command <- request.body.toCommand(userOrg.organisationReference)
            _ = systemServices.timeBookingViewService ! command
          } yield Ok
        }
    }
}
