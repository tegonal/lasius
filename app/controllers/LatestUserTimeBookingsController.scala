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

import models.UserId
import play.api.mvc.Action
import core.Global._
import domain.views.LatestUserTimeBookingsView._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json._
import models.FreeUser
import scala.concurrent.Future

class LatestUserTimeBookingsController {
  self: Controller with Security =>

  def getLatestTimeBooking(maxHistory: Int) = HasRole(FreeUser, parse.empty) {
    implicit subject =>
      implicit request => {
        latestUserTimeBookingsViewService ! GetLatestTimeBooking(subject.userId, maxHistory)
        Future.successful(Ok)
      }
  }
}

object LatestUserTimeBookingsController extends LatestUserTimeBookingsController with Controller with Security with DefaultSecurityComponent