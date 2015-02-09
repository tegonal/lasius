/**
 * Current Time Booking service
 */
define(['angular'], function (angular) {
  'use strict';

  var mod = angular.module('services.currentTimeBooking', []);
  mod.factory('currentTimeBookingService', ['$http', '$location', '$q', 'playRoutes', '$log', function ($http, $location, $q, playRoutes, $log) {
    
    return {             
      getCurrentTimeBooking: function (userId) {
        return playRoutes.controllers.CurrentUserTimeBookingsController.getCurrentTimeBooking(userId).get().then(function (response) {
          return response.data;          
        }, function(reason) {
          $log.debug("Failed loading document:"+reason);
          return reason.data;
        });
      },
      stopBooking: function(userId, bookingId) {
        return playRoutes.controllers.TimeBookingController.stop(userId, bookingId).get().then(function (response) {
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
