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

import play.api.mvc._
import play.api.routing.JavaScriptReverseRouter
import play.mvc.Http.MimeTypes

class JSController extends Controller {

  /**
   * Returns the JavaScript router that the client can use for "type-safe" routes.
   * Uses browser caching; set duration (in seconds) according to your release cycle.
   */
  def javascriptRoutes = Action { implicit request =>
    Ok(
      JavaScriptReverseRouter("jsRoutes")(
        routes.javascript.ApplicationController.index,
        routes.javascript.ApplicationController.login,
        routes.javascript.ApplicationController.logout,
        routes.javascript.ApplicationController.messagingSocket,
        routes.javascript.ApplicationController.config,
        routes.javascript.CurrentUserTimeBookingsController.getCurrentTimeBooking,
        routes.javascript.CurrentTeamTimeBookingsController.getTeamTimeBooking,
        routes.javascript.LatestUserTimeBookingsController.getLatestTimeBooking,
        routes.javascript.StructureController.getCategories,
        routes.javascript.TimeBookingController.add,
        routes.javascript.TimeBookingController.changeStart,
        routes.javascript.TimeBookingController.edit,
        routes.javascript.TimeBookingController.pause,
        routes.javascript.TimeBookingController.remove,
        routes.javascript.TimeBookingController.resume,
        routes.javascript.TimeBookingController.start,
        routes.javascript.TimeBookingController.stop,
        routes.javascript.TimeBookingHistoryController.getTimeBookingHistory,
        routes.javascript.TimeBookingHistoryController.getTimeBookingHistoryByRange,
        routes.javascript.TimeBookingHistoryController.exportTimeBookingHistory,
        routes.javascript.TimeBookingHistoryController.exportTimeBookingHistoryByRange,
        routes.javascript.TimeBookingStatisticsController.getAggregatedStatistics,
        routes.javascript.TimeBookingStatisticsController.getStatistics,
        routes.javascript.UserFavoritesController.addFavorite,
        routes.javascript.UserFavoritesController.getFavorites,
        routes.javascript.UserFavoritesController.removeFavorite,
        routes.javascript.UsersController.authUser)).as(MimeTypes.JAVASCRIPT)
  }
}