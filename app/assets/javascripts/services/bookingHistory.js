/**
 * Current Time Booking service
 */
define(['angular'], function (angular) {
  'use strict';

  var mod = angular.module('services.bookingHistory', []);
  mod.factory('bookingHistoryService', ['$http', '$location', '$q', 'playRoutes', '$log', function ($http, $location, $q, playRoutes, $log) {
    
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
        return playRoutes.controllers.TimeBookingController.edit(booking.id, booking.start, booking.end).post().then(function (response) {
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
