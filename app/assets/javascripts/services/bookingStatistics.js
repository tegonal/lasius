/**
 * Current Time Booking service
 */
define(['angular'], function (angular) {
  'use strict';

  var mod = angular.module('services.bookingStatistics', []);
  mod.factory('bookingStatisticsService', ['$http', '$location', '$q', 'playRoutes', '$log', function ($http, $location, $q, playRoutes, $log) {
    
    return {             
      getAggregatedStatistics: function (source, userId, from, to) {
        return playRoutes.controllers.TimeBookingStatisticsController.getAggregatedStatistics(source, userId, from, to).get().then(function (response) {
          return response.data;
        }, function(reason) {
          $log.debug("Failed loading document:"+reason);
          return reason.data;
        });
      },
      getStatistics: function (source, userId, from, to) {
        return playRoutes.controllers.TimeBookingStatisticsController.getStatistics(source, userId, from, to).get().then(function (response) {
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
