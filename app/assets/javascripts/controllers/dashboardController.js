/**
 * Booking controller
 */
define(['angular'], function(angular) {
  'use strict';

  var DashboardCtrl = function($q, $log, $scope, $rootScope) {
    $scope.test = "test";
  };
  
  DashboardCtrl.$inject = ['$q', '$log', '$scope', '$rootScope'];

  return {
    DashboardCtrl: DashboardCtrl
  };

});
