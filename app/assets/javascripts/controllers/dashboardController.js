/**
 * Booking controller
 */
define(['angular'], function(angular) {
  'use strict';

  var DashboardCtrl = function($q, $log, $scope, $rootScope, $animate, $document, moment) {
    
    $scope.date = moment();
    $scope.currentBookingOn = true;
    
    $scope.showCurrentBooking = function(on) {
      $scope.currentBookingOn = on;
      var body = $document[0].body;
      var content = $document[0].getElementById('content_body');
      if (on) {
        $animate.setClass(content, 'main_content', 'main_content_closed');
      }
      else {        
        $animate.setClass(content, 'main_content_closed', 'main_content');
      }
    };
    
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
  
  DashboardCtrl.$inject = ['$q', '$log', '$scope', '$rootScope', '$animate', '$document', 'moment'];  

  return {
    DashboardCtrl: DashboardCtrl
  };

});
