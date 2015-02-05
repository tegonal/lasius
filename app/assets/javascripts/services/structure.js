/**
 * Current Time Booking service
 */
define(['angular'], function (angular) {
  'use strict';

  var mod = angular.module('services.structure', []);
  mod.factory('structureService', ['$http', '$location', '$q', 'playRoutes', '$log', function ($http, $location, $q, playRoutes, $log) {
    
    return {             
      getCurrentTimeBooking: function (userId) {
        return playRoutes.controllers.StructureController.getCategories(userId).get().then(function (response) {
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
