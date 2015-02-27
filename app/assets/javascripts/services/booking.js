/**
 * Current Time Booking service
 */
define(['angular'], function (angular) {
  'use strict';

  var mod = angular.module('services.booking', []);
  mod.factory('bookingService', ['$http', '$location', '$q', 'playRoutes', '$log', function ($http, $location, $q, playRoutes, $log) {
    
    return {             
      getCategories: function (userId) {
        return playRoutes.controllers.StructureController.getCategories(userId).get().then(function (response) {
          return response.data;          
        }, function(reason) {
          $log.debug("Failed loading document:"+reason);
          return reason.data;
        });
      },
      start: function(userId, categoryId, projectId, tags) {
        return playRoutes.controllers.TimeBookingController.start(userId, categoryId, projectId, tags).post().then(function (response) {
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
