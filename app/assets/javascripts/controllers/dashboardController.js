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
    
    $scope.contentTypeMapping = {
        "workview": "assets/workview.html",
        "bookings": "assets/bookings.html",
        "statistics": "assets/statistics.html"
    };
    
    $scope.contentType = "workview";
    $scope.selectContentType = function (contentType) {
      $scope.contentType = contentType;      
    };
  };
  
  DashboardCtrl.$inject = ['$q', '$log', '$scope', '$rootScope', 'moment'];  

  return {
    DashboardCtrl: DashboardCtrl
  };

});
