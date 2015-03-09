/**
 * Current Time Booking service
 */
define(['angular'], function (angular) {
  'use strict';

  var mod = angular.module('services.currentTimeBooking', []);
  mod.factory('currentTimeBookingService', ['$http', '$location', '$q', 'playRoutes', '$log', function ($http, $location, $q, playRoutes, $log) {
    
    return {     
      getCurrentTimeBooking: function () {
        return playRoutes.controllers.CurrentUserTimeBookingsController.getCurrentTimeBooking().get().then(function (response) {
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
      }
    };
  }]);
 
  return mod;
});
