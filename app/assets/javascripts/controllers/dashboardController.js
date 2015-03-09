/**
 * Booking controller
 */
define(['angular'], function(angular) {
  'use strict';

  var DashboardCtrl = function($q, $log, $scope, $rootScope, $animate, $document, moment, userService) {
        
    $scope.bookingRange = {};    
    $scope.statisticRange = {};    
    $scope.currentBookingOn = true;
    
    $scope.$watch(function() {
      return userService.getUser();
    }, function(user) {
      if (angular.isDefined(user)) {
        $log.debug('Got user:'+user);
        $scope.user = user;
      }
    }, true);
    
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
        
    $scope.contentTypeMapping = {
        "workview": "assets/workview.html",
        "bookings": "assets/bookings.html",
        "statistics": "assets/statistics.html"
    };
    
    $scope.contentType = "workview";
    $scope.selectContentType = function (contentType, currentBookingOn) {
      $scope.contentType = contentType;      
      $scope.showCurrentBooking(currentBookingOn);
    };
  };
  
  DashboardCtrl.$inject = ['$q', '$log', '$scope', '$rootScope', '$animate', '$document', 'moment', 'userService'];  

  return {
    DashboardCtrl: DashboardCtrl
  };

});
