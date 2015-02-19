/**
 * Booking controller
 */
define(['angular'], function(angular) {
  'use strict';

  var DashboardCtrl = function($q, $log, $scope, $rootScope, moment) {
    
    $scope.date = moment();
    
    $scope.dayMinus = function() {
      $scope.date = $scope.date.subtract(1, 'day');
    };
    $scope.dayPlus = function() {
      $scope.date = $scope.date.add(1, 'day');
    };
    
    $scope.dateSelection = function() {
      return function(){ return $scope.date;};
    };
  };
  
  DashboardCtrl.$inject = ['$q', '$log', '$scope', '$rootScope', 'moment'];  

  return {
    DashboardCtrl: DashboardCtrl
  };

});
