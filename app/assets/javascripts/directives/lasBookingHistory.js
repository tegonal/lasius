
define(['angular'], function(angular) {
  'use strict';

  var mod = angular.module('directives.lasBookingHistory', []);
  mod.directive('lasBookingHistory', ['bookingHistoryService', 'moment', function(bookingHistoryService, moment) {
    return {
      restrict: 'E',
      transclude: true,
      templateUrl: '/assets/directives/las-booking-history-tmpl.html',
      scope:  {
        userId:'='
      },
      link: function(scope, iElement, iAttrs) {
        
        scope.date = moment();
        
        
        var pattern = 'DDMMYYYYHHmmss';               
        
        var load = function() {
          var from = scope.date.startOf('day').format(pattern);
          var to = scope.date.endOf('day').format(pattern);
          
          bookingHistoryService.getTimeBookingHistory(scope.userId, from, to).then(function(bookings) {
            scope.bookings = bookings;          
          });
        };
        load();
        
        scope.dayMinus = function() {
          scope.date = scope.date.subtract(1, 'day');
          load();
        };
        scope.dayPlus = function() {
          scope.date = scope.date.add(1, 'day');
          load();
        };
        
        scope.diff = function(booking) {
          return moment.duration(booking.end).subtract(booking.start).asHours();
        };
      }
    };
  }]);
  return mod;
});

