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
package controllers

import akka.pattern.ask
import core.{DefaultCacheAware, DefaultSystemServicesAware, SystemServicesAware}
import domain.views.CurrentTeamTimeBookingsView._
import models._
import play.api.libs.json._
import play.api.mvc.Controller
import play.api.Logger

class CurrentTeamTimeBookingsController {
  self: Controller with Security with SystemServicesAware =>

  lazy val logger = Logger(getClass().getName())

  def getTeamTimeBooking(teamId: TeamId) = HasRole(FreeUser, parse.empty) {
    implicit subject =>
      implicit request => {
        systemServices.currentTeamTimeBookingsView ? GetCurrentTeamTimeBookings(teamId) map {
          case b: CurrentTeamTimeBookings =>
            logger.debug(s"getCurrentTeamTimeBooking:$b")
            Ok(Json.toJson(b))
          case NoResultFound =>
            logger.debug(s"getCurrentTeamTimeBooking: NoResultFound")
            NotFound
          case x =>
            logger.debug(s"getCurrentTeamTimeBooking:$teamId => $x")
            BadRequest
        }
      }
  }
}

object CurrentTeamTimeBookingsController extends CurrentTeamTimeBookingsController with Controller with Security with DefaultSecurityComponent with DefaultCacheAware with DefaultSystemServicesAware