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
import org.joda.time.DateTime
import org.pac4j.core.context.session.SessionStore
import org.pac4j.play.scala.SecurityComponents
import play.api.mvc.Action
import play.modules.reactivemongo.ReactiveMongoApi

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TimeBookingController @Inject() (
    override val controllerComponents: SecurityComponents,
    override val authConfig: AuthConfig,
    override val reactiveMongoApi: ReactiveMongoApi,
    override val systemServices: SystemServices,
    override val playSessionStore: SessionStore)(implicit ec: ExecutionContext)
    extends BaseLasiusController(controllerComponents) {

  override val supportTransaction: Boolean = systemServices.supportTransaction

  implicit val timeout: Timeout = systemServices.timeout

  def start(orgId: OrganisationId): Action[StartBookingRequest] =
    HasUserRole(FreeUser,
                validateJson[StartBookingRequest],
                withinTransaction = false) {
      implicit dbSession => implicit subject => user => implicit request =>
        HasOrganisationRole(user, orgId, OrganisationMember) { userOrg =>
          HasProjectRole(userOrg, request.body.projectId, ProjectMember) {
            userProject =>
              val startBooking = request.body
              logger.debug(
                s"TimeBokingController -> start - userId:${subject.userReference.id}, projectId: ${startBooking.projectId.value}, tags:${startBooking.tags}, start:${startBooking.start}")

              systemServices.timeBookingViewService ! startBooking.toCommand(
                userOrg.organisationReference,
                userProject.projectReference)
              success()
          }
        }
    }

  def stop(orgId: OrganisationId,
           bookingId: BookingId): Action[StopBookingRequest] =
    HasUserRole(FreeUser,
                validateJson[StopBookingRequest],
                withinTransaction = false) {
      implicit dbSession => implicit subject => user => implicit request =>
        HasOrganisationRole(user, orgId, OrganisationMember) { userOrg =>
          systemServices.timeBookingViewService ! EndBookingCommand(
            subject.userReference,
            userOrg.organisationReference,
            bookingId,
            request.body.end.getOrElse(DateTime.now()))
          success()
        }
    }

  def remove(orgId: OrganisationId, bookingId: BookingId): Action[Unit] =
    HasUserRole(FreeUser, parse.empty, withinTransaction = false) {
      implicit dbSession => implicit subject => user => implicit request =>
        HasOrganisationRole(user, orgId, OrganisationMember) {
          userOrganisation =>
            systemServices.timeBookingViewService ! RemoveBookingCommand(
              subject.userReference,
              userOrganisation.organisationReference,
              bookingId)
            success()
        }
    }

  def edit(orgId: OrganisationId,
           bookingId: BookingId): Action[EditBookingRequest] =
    HasUserRole(FreeUser,
                validateJson[EditBookingRequest],
                withinTransaction = false) {
      implicit dbSession => implicit subject => user => implicit request =>
        HasOrganisationRole(user, orgId, OrganisationMember) { userOrg =>
          HasOptionalProjectRole(userOrg,
                                 request.body.projectId,
                                 ProjectMember) { maybeUserProject =>
            for {
              _ <- request.body.start
                .flatMap(start =>
                  request.body.end.flatten.map(end =>
                    validateStartBeforeEnd(start, end)))
                .getOrElse(Future.successful(Ok))
              _ = systemServices.timeBookingViewService ! request.body
                .toCommand(bookingId,
                           userOrg.organisationReference,
                           maybeUserProject.map(_.projectReference))
            } yield Ok
          }
        }
    }

  def changeStart(orgId: OrganisationId,
                  bookingId: BookingId): Action[BookingChangeStartRequest] =
    HasUserRole(FreeUser,
                validateJson[BookingChangeStartRequest],
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

  def add(orgId: OrganisationId): Action[AddBookingRequest] =
    HasUserRole(FreeUser,
                validateJson[AddBookingRequest],
                withinTransaction = false) {
      implicit dbSession => implicit subject => user => implicit request =>
        HasOrganisationRole(user, orgId, OrganisationMember) { userOrg =>
          HasProjectRole(userOrg, request.body.projectId, ProjectMember) {
            userProject =>
              for {
                _ <- validateStartBeforeEnd(request.body.start,
                                            request.body.end)
                _ = systemServices.timeBookingViewService ! request.body
                  .toCommand(userOrg.organisationReference,
                             userProject.projectReference)
              } yield Ok
          }
        }
    }
}
