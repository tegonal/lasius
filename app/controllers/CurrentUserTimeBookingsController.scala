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
import domain.views.CurrentUserTimeBookingsView._
import models._
import play.api.libs.json._
import play.api.mvc.Controller
import play.api.Logger

class CurrentUserTimeBookingsController {
  self: Controller with Security with SystemServicesAware =>

  def getCurrentTimeBooking() = HasRole(FreeUser, parse.empty) {
    implicit subject =>
      implicit request => {
        systemServices.currentUserTimeBookingsViewService ? GetCurrentTimeBooking(subject.userId) map {
          case c:CurrentUserTimeBookingEvent => 
            Ok(Json.toJson(c))
          case x =>
            Logger.debug(s"getCurrentTimeBooking:${subject.userId} => $x")
            BadRequest
        }
      }
  }
}

object CurrentUserTimeBookingsController extends CurrentUserTimeBookingsController with Controller with Security with DefaultSecurityComponent with DefaultCacheAware with DefaultSystemServicesAware