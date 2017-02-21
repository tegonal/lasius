/*   __                          __                                          *\
*   / /____ ___ ____  ___  ___ _/ /       lasius                      *
*  / __/ -_) _ `/ _ \/ _ \/ _ `/ /        contributed by tegonal              *
*  \__/\__/\_, /\___/_//_/\_,_/_/         http://tegonal.com/                 *
*         /___/                                                               *
*                                                                             *
* This program is free software: you can redistribute it and/or modify it     *
* under the terms of the GNU General Public License as published by    *
* the Free Software Foundation, either version 3 of the License,              *
* or (at your option) any later version.                                      *
*                                                                             *
* This program is distributed in the hope that it will be useful, but         *
* WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY  *
* or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for *
* more details.                                                               *
*                                                                             *
* You should have received a copy of the GNU General Public License along     *
* with this program. If not, see http://www.gnu.org/licenses/                 *
*                                                                             *
\*                                                                           */
define(['angular'], function(angular) {
  'use strict';

  var DashboardCtrl = function($q, $log, $scope, $rootScope, $animate, $document, moment, userService, Auth, appConfigService) {
        
    $scope.bookingRange = {};    
    $scope.statisticRange = {};    
    $scope.currentBookingOn = true;
    
    appConfigService.resolveConfig().then(function(config) {
      $scope.appConfig = config;
    });
    
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
    
    $scope.toggleCurrentBooking = function() {
      $scope.showCurrentBooking(!$scope.currentBookingOn);
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
  
  DashboardCtrl.$inject = ['$q', '$log', '$scope', '$rootScope', '$animate', '$document', 'moment', 'userService', 'Auth'];  

  return {
    DashboardCtrl: DashboardCtrl
  };

});
