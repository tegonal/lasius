
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
        
        var pattern = 'DDMMYYYYHHmmss';
        var from = moment().startOf('day').format(pattern);
        var to = moment().endOf('day').format(pattern);
        
        bookingHistoryService.getTimeBookingHistory(scope.userId, from, to).then(function(bookings) {
          scope.bookings = bookings;          
        });
        
        scope.diff = function(booking) {
          return moment(booking.end).subtract(booking.start);
        };
      }
    };
  }]);
  return mod;
});

