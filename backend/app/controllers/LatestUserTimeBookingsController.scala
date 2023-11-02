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
import domain.views.LatestUserTimeBookingsView._
import models.{FreeUser, OrganisationId, OrganisationMember}
import org.pac4j.core.context.session.SessionStore
import org.pac4j.play.scala.SecurityComponents
import play.api.mvc.Action
import play.modules.reactivemongo.ReactiveMongoApi

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class LatestUserTimeBookingsController @Inject() (
    override val controllerComponents: SecurityComponents,
    override val systemServices: SystemServices,
    override val authConfig: AuthConfig,
    override val reactiveMongoApi: ReactiveMongoApi,
    override val playSessionStore: SessionStore)(implicit ec: ExecutionContext)
    extends BaseLasiusController(controllerComponents) {

  implicit val timeout: Timeout = systemServices.timeout

  def getLatestTimeBooking(orgId: OrganisationId,
                           maxHistory: Int): Action[Unit] =
    HasUserRole(FreeUser, parse.empty, withinTransaction = false) {
      implicit dbSession => implicit subject => user => implicit request =>
        HasOrganisationRole(user, orgId, OrganisationMember) { _ =>
          systemServices.latestUserTimeBookingsViewService ! GetLatestTimeBooking(
            subject.userReference,
            maxHistory)
          Future.successful(Ok)
        }
    }
}
