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
import core.Validation.ValidationFailedException
import models._
import org.joda.time._
import org.pac4j.core.context.session.SessionStore
import org.pac4j.play.scala.{DefaultSecurityComponents, SecurityComponents}
import play.api.libs.json._
import play.api.mvc.Action
import play.modules.reactivemongo.ReactiveMongoApi
import repositories.{BookingByProjectRepository, BookingByTagRepository}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TimeBookingStatisticsController @Inject() (
    override val controllerComponents: SecurityComponents,
    override val systemServices: SystemServices,
    override val authConfig: AuthConfig,
    override val reactiveMongoApi: ReactiveMongoApi,
    override val playSessionStore: SessionStore,
    val bookingByProjectRepository: BookingByProjectRepository,
    val bookingByTagRepository: BookingByTagRepository)(implicit
    ec: ExecutionContext)
    extends BaseLasiusController(controllerComponents) {

  implicit val timeout: Timeout = systemServices.timeout

  def getAggregatedStatisticsByUser(orgId: OrganisationId,
                                    source: String,
                                    from: LocalDate,
                                    to: LocalDate,
                                    granularity: Granularity): Action[Unit] =
    HasUserRole(FreeUser, parse.empty, withinTransaction = false) {
      implicit dbSession => implicit subject => user => implicit request =>
        HasOrganisationRole(user, orgId, OrganisationMember) { _ =>
          logger.debug(
            s"getAggregatedStatisticsByUser, source:$source, userId:${subject.userReference}, orgId: ${orgId.value}, from:$from, to:$to")
          source match {
            case "tag" =>
              bookingByTagRepository
                .findAggregatedByUserAndRange(subject.userReference,
                                              orgId,
                                              from,
                                              to,
                                              "tagId",
                                              granularity)
                .map(result => Ok(Json.toJson(result)))
            case "project" =>
              bookingByProjectRepository
                .findAggregatedByUserAndRange(subject.userReference,
                                              orgId,
                                              from,
                                              to,
                                              "projectReference.key",
                                              granularity)
                .map(result => Ok(Json.toJson(result)))
            case s =>
              Future.failed(ValidationFailedException(s"Unsupported source:$s"))
          }
        }
    }

  def getAggregatedStatisticsByOrganisation(
      orgId: OrganisationId,
      source: String,
      from: LocalDate,
      to: LocalDate,
      granularity: Granularity): Action[Unit] =
    HasUserRole(FreeUser, parse.empty, withinTransaction = false) {
      implicit dbSession => implicit subject => user => implicit request =>
        HasOrganisationRole(user, orgId, OrganisationAdministrator) { _ =>
          logger.debug(
            s"getAggregatedStatisticsByOrganisation, source:$source, orgId: ${orgId.value}, from:$from, to:$to")
          source match {
            case "tag" =>
              bookingByTagRepository
                .findAggregatedByOrganisationAndRange(orgId,
                                                      from,
                                                      to,
                                                      "tagId",
                                                      granularity)
                .map(result => Ok(Json.toJson(result)))
            case "project" =>
              bookingByProjectRepository
                .findAggregatedByOrganisationAndRange(orgId,
                                                      from,
                                                      to,
                                                      "projectReference.key",
                                                      granularity)
                .map(result => Ok(Json.toJson(result)))
            case "user" =>
              bookingByProjectRepository
                .findAggregatedByOrganisationAndRange(orgId,
                                                      from,
                                                      to,
                                                      "userReference.key",
                                                      granularity)
                .map(result => Ok(Json.toJson(result)))
            case s =>
              Future.failed(ValidationFailedException(s"Unsupported source:$s"))
          }
        }
    }

  def getAggregatedStatisticsByProject(orgId: OrganisationId,
                                       projectId: ProjectId,
                                       source: String,
                                       from: LocalDate,
                                       to: LocalDate,
                                       granularity: Granularity): Action[Unit] =
    HasUserRole(FreeUser, parse.empty, withinTransaction = false) {
      implicit dbSession => implicit subject => user => implicit request =>
        HasOrganisationRole(user, orgId, OrganisationMember) { userOrg =>
          HasProjectRole(userOrg, projectId, ProjectAdministrator) { _ =>
            logger.debug(
              s"getAggregatedStatisticsByOrganisation, source:$source, orgId: ${orgId.value}, from:$from, to:$to")
            source match {
              case "tag" =>
                bookingByTagRepository
                  .findAggregatedByProjectAndRange(projectId,
                                                   from,
                                                   to,
                                                   "tagId",
                                                   granularity)
                  .map(result => Ok(Json.toJson(result)))
              case "project" =>
                bookingByProjectRepository
                  .findAggregatedByProjectAndRange(projectId,
                                                   from,
                                                   to,
                                                   "projectReference.key",
                                                   granularity)
                  .map(result => Ok(Json.toJson(result)))
              case "user" =>
                bookingByProjectRepository
                  .findAggregatedByProjectAndRange(projectId,
                                                   from,
                                                   to,
                                                   "userReference.key",
                                                   granularity)
                  .map(result => Ok(Json.toJson(result)))
              case "organisation" =>
                bookingByProjectRepository
                  .findAggregatedByProjectAndRange(projectId,
                                                   from,
                                                   to,
                                                   "organisationReference.key",
                                                   granularity)
                  .map(result => Ok(Json.toJson(result)))
              case s =>
                Future.failed(
                  ValidationFailedException(s"Unsupported source:$s"))
            }
          }
        }
    }
}
