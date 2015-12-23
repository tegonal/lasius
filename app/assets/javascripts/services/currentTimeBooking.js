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

  var mod = angular.module('services.currentTimeBooking', []);
  mod.factory('currentTimeBookingService', ['$http', '$location', '$q', 'playRoutes', '$log', '$rootScope', 'msgBus', function ($http, $location, $q, playRoutes, $log, $rootScope, msgBus) {
    var currentTimeBooking;
    
    msgBus.onMsg('CurrentUserTimeBookingEvent', $rootScope, function(
        event, msg) {
      currentTimeBooking = msg.booking;
      $rootScope.$apply();
    });
    
    var loadCurrentTimeBooking = function () {
      return playRoutes.controllers.CurrentUserTimeBookingsController.getCurrentTimeBooking().get().then(function (response) {
        return response.data;          
      }, function(reason) {
        $log.debug("Failed loading document:"+reason);
        return reason.data;
      });
    };
    
    return {
      getCurrentTimeBooking: function() {
        return currentTimeBooking;
      },      
      resolveCurrentTimeBooking: function(reload) {
        var deferred = $q.defer();        
        if (currentTimeBooking && reload) {
          deferred.resolve(currentTimeBooking);
        }
        else {          
          loadCurrentTimeBooking().then(function(currentTimeBookingEvent){
            currentTimeBooking = currentTimeBookingEvent.booking;
            deferred.resolve(currentTimeBooking);
          });            
        }
      },      
      getTeamTimeBooking: function (teamId) {        
        return playRoutes.controllers.CurrentTeamTimeBookingsController.getTeamTimeBooking(teamId).get().then(function (response) {
          return response.data;          
        }, function(reason) {
          $log.debug("Failed loading document:"+reason);
          return reason.data;
        });
      },
      stopBooking: function(bookingId) {
        return playRoutes.controllers.TimeBookingController.stop(bookingId).post().then(function (response) {
          return response.data;          
        }, function(reason) {
          $log.debug("Failed loading document:"+reason);
          return reason.data;
        });
      },
      pauseBooking: function(bookingId, time) {
        return playRoutes.controllers.TimeBookingController.pause(bookingId, time).post().then(function (response) {
          return response.data;          
        }, function(reason) {
          $log.debug("Failed loading document:"+reason);
          return reason.data;
        });
      },
      resumeBooking: function(bookingId, time) {
        return playRoutes.controllers.TimeBookingController.resume(bookingId, time).post().then(function (response) {
          return response.data;          
        }, function(reason) {
          $log.debug("Failed loading document:"+reason);
          return reason.data;
        });
      },
      changeStartTime: function(bookingId, start) {
        return playRoutes.controllers.TimeBookingController.changeStart(bookingId, start).post().then(function (response) {
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
