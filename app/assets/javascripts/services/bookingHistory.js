/**
 * Current Time Booking service
 */
define(['angular'], function (angular) {
  'use strict';

  var mod = angular.module('services.bookingHistory', []);
  mod.factory('bookingHistoryService', ['$http', '$location', '$q', 'playRoutes', '$log', function ($http, $location, $q, playRoutes, $log) {
    
    return {             
      getTimeBookingHistory: function (userId, from, to) {
        return playRoutes.controllers.TimeBookingHistoryController.getTimeBookingHistory(userId, from, to).get().then(function (response) {
          return response.data;          
        }, function(reason) {
          $log.debug("Failed loading document:"+reason);
          return reason.data;
        });
      },
      removeTimeBooking: function (userId, bookingId) {
        return playRoutes.controllers.TimeBookingController.remove(userId, bookingId).get().then(function (response) {
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
