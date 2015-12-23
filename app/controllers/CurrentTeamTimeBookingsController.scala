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

import play.api.mvc.Controller
import models._
import play.api.mvc.Action
import core.Global._
import domain.views.CurrentUserTimeBookingsView._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json._
import scala.concurrent.Future
import domain.views.CurrentTeamTimeBookingsView._
import play.api.Logger

class CurrentTeamTimeBookingsController {
  self: Controller with Security =>

  def getTeamTimeBooking(teamId: TeamId) = HasRole(FreeUser, parse.empty) {
    implicit subject =>
      implicit request => {
        currentTeamTimeBookingsView ? GetCurrentTeamTimeBookings(teamId) map {
          case b: CurrentTeamTimeBookings =>
            Logger.debug(s"getCurrentTeamTimeBooking:$b")
            Ok(Json.toJson(b))
          case NoResultFound =>
            Logger.debug(s"getCurrentTeamTimeBooking: NoResultFound")
            NotFound
          case x => 
            Logger.debug(s"getCurrentTeamTimeBooking:$teamId => $x")
            BadRequest
        }
      }
  }
}

object CurrentTeamTimeBookingsController extends CurrentTeamTimeBookingsController with Controller with Security with DefaultSecurityComponent