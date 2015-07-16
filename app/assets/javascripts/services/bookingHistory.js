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
define(['angular'], function (angular) {
  'use strict';

  var mod = angular.module('services.bookingHistory', []);
  mod.factory('bookingHistoryService', ['$http', '$location', '$q', 'playRoutes', '$log', 'MY_CONFIG', 'moment', function ($http, $location, $q, playRoutes, $log, MY_CONFIG, moment) {
    
    return {             
      getTimeBookingHistory: function (from, to) {
        return playRoutes.controllers.TimeBookingHistoryController.getTimeBookingHistory(from, to).get().then(function (response) {
          return response.data;          
        }, function(reason) {
          $log.debug("Failed loading document:"+reason);
          return reason.data;
        });
      },
      removeTimeBooking: function (bookingId) {
        return playRoutes.controllers.TimeBookingController.remove(bookingId).delete().then(function (response) {
          return response.data;          
        }, function(reason) {
          $log.debug("Failed loading document:"+reason);
          return reason.data;
        });
      },
      editTimeBooking: function (booking) {
        var start = moment(booking.start).format(MY_CONFIG.DATE_PATTERN);
        var end = moment(booking.end).format(MY_CONFIG.DATE_PATTERN);
        
        return playRoutes.controllers.TimeBookingController.edit(booking.id, start, end).post().then(function (response) {
          return response.data;          
        }, function(reason) {
          $log.debug("Failed loading document:"+reason);
          return reason.data;
        });
      },
      addTimeBooking: function (booking) {
        var start = moment(booking.start).format(MY_CONFIG.DATE_PATTERN);
        var end = moment(booking.end).format(MY_CONFIG.DATE_PATTERN);
        
        var tagStrings = [];
        angular.forEach(booking.tags, function(value, key) {
          this.push(value.id);
        }, tagStrings);
        
        return playRoutes.controllers.TimeBookingController.add(booking.project.categoryId, booking.project.project.id, tagStrings, start, end, booking.comment).post().then(function (response) {
          return response.data;          
        }, function(reason) {
          $log.debug("Failed loading document:"+reason);
          return reason.data;
        });
      }
    };
  }]);
 
  return mod;
});
