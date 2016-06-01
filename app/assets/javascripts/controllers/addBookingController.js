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

  var AddBookingCtrl = function($q, $log, $scope, $rootScope, $modalInstance, moment, MY_CONFIG, bookingService) {
    
    $scope.today = new Date();
    
    var nextWeek = new Date();
    nextWeek.setDate(nextWeek.getDate() + 7);

    $scope.maxDate = nextWeek;
    
    $scope.format = MY_CONFIG.DATE_FORMAT;
    
    $scope.day = moment();
    
    $scope.dateOptions = {
        formatYear: 'yyyy',
        startingDay: 1,
        showWeeks: true
      };
    
    $scope.ok = function () {
      //merge dates with current selected day
      var start = moment.tz($scope.booking.start, MY_CONFIG.TIMEZONE);
      var end = moment.tz($scope.booking.end, MY_CONFIG.TIMEZONE);
      $scope.booking.start = start.seconds(0).milliseconds(0).toDate();
      $scope.booking.end = end.seconds(0).milliseconds(0).toDate();
      
      $modalInstance.close($scope.booking);
    };

    $scope.cancel = function () {
      $modalInstance.dismiss('cancel');
    };
    
    $scope.projects = [];
    $scope.availableTags = [];
    $scope.booking = {
        project: '',
        tags: {},
        comment: '',
        start: $scope.today,
        end: $scope.today
    };
    
    //noting to do here
    bookingService.getCategories().then(function(projects) {
      $scope.projects = projects;          
    });
    
    $scope.projectSelectionChanged = function() {
       $scope.availableTags = $scope.booking.project.project.tags;
       $scope.tags = {};
    };
    
    $scope.newTag = function(name) {
      return {id: name};          
    };
  };
  
  AddBookingCtrl.$inject = ['$q', '$log', '$scope', '$rootScope', '$modalInstance', 'moment', 'MY_CONFIG', 'bookingService'];  

  return {
    AddBookingCtrl: AddBookingCtrl
  };

});
