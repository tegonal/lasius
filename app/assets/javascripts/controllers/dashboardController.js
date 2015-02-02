/**
 * Booking controller
 */
define(['angular'], function(angular) {
  'use strict';

  var DashboardCtrl = function($q, $log, $scope, $rootScope, currentTimeBookingService) {
        $scope.currentTimeBookingService = currentTimeBookingService;
        
        currentTimeBookingService.getCurrentTimeBooking('noob').then(function(data) {
          $scope.currentTimeBooking = data.booking;
        });
  };
  
  DashboardCtrl.$inject = ['$q', '$log', '$scope', '$rootScope', 'currentTimeBookingService'];  

  return {
    DashboardCtrl: DashboardCtrl
  };

});
