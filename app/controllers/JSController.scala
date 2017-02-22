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

import play.api.Play.current
import play.api.cache.Cached
import play.api.mvc.Action
import play.api.mvc.Controller
import play.api.routing.JavaScriptReverseRouter

object JSController extends Controller {

  /**
   * Returns the JavaScript router that the client can use for "type-safe" routes.
   * Uses browser caching; set duration (in seconds) according to your release cycle.
   * @param varName The name of the global variable, defaults to `jsRoutes`
   */
  def jsRoutes(varName: String = "jsRoutes") =
    Cached(_ => "jsRoutes", duration = 86400) {
      Action { implicit request =>
        import routes.javascript._
        Ok(
          JavaScriptReverseRouter("jsRoutes")(
            ApplicationController.index,
            ApplicationController.login,
            ApplicationController.logout,
            ApplicationController.messagingSocket,
            ApplicationController.config,
            CurrentUserTimeBookingsController.getCurrentTimeBooking,
            CurrentTeamTimeBookingsController.getTeamTimeBooking,
            LatestUserTimeBookingsController.getLatestTimeBooking,
            StructureController.getCategories,
            TimeBookingController.add,
            TimeBookingController.changeStart,
            TimeBookingController.edit,
            TimeBookingController.pause,
            TimeBookingController.remove,
            TimeBookingController.resume,
            TimeBookingController.start,
            TimeBookingController.stop,
            TimeBookingHistoryController.getTimeBookingHistory,
            TimeBookingHistoryController.exportTimeBookingHistory,
            TimeBookingStatisticsController.getAggregatedStatistics,
            TimeBookingStatisticsController.getStatistics,
            UserFavoritesController.addFavorite,
            UserFavoritesController.getFavorites,
            UserFavoritesController.removeFavorite,
            UsersController.authUser)).as(JAVASCRIPT)
      }
    }
}