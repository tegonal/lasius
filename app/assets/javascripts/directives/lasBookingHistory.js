
define(['angular'], function(angular) {
  'use strict';

  var mod = angular.module('directives.lasBookingHistory', []);
  mod.directive('lasBookingHistory', ['bookingHistoryService', 'msgBus', 'moment', function(bookingHistoryService, msgBus, moment) {
    return {
      restrict: 'E',
      transclude: true,
      templateUrl: '/assets/directives/las-booking-history-tmpl.html',
      scope:  {
        userId:'=',
        range:'='
      },
      link: function(scope, iElement, iAttrs) {
        
        var pattern = 'DDMMYYYYHHmmss';               
        
        var load = function(range) {
          if (range === undefined || range.from === undefined) {
            return;
          }
          var from = range.from.format(pattern);
          var to = range.to.format(pattern);
          
          bookingHistoryService.getTimeBookingHistory(scope.userId, from, to).then(function(bookings) {
            scope.bookings = bookings;          
          });
        };
        
        scope.diff = function(booking) {
          return moment.duration(booking.end).subtract(booking.start).asHours();
        };
        
        scope.removeTimeBooking = function(bookingId) {
          bookingHistoryService.removeTimeBooking(scope.userId, bookingId);
        };
        
        scope.$watch('range',
            function(value){
              load(value);                
            }, true);
        
        msgBus.onMsg('UserTimeBookingHistoryEntryAdded', scope, function(
            event, msg) {
          console.log('msg received' + msg.type);
          if (scope.userId === msg.booking.userId && scope.range.from.unix() === moment(msg.booking.start).startOf('day').unix()) {
            scope.bookings.push(msg.booking);
            scope.$apply();
          }          
        });
        
        msgBus.onMsg('UserTimeBookingHistoryEntryRemoved', scope, function(
            event, msg) {
          console.log('msg received' + msg.type);
          for(var i=0;i<scope.bookings.length;i++){
            if(scope.bookings[i].id === msg.bookingId){
              scope.bookings.splice(i, 1);
            }
          }               
        });
        
        msgBus.onMsg('UserTimeBookingHistoryEntryCleaned', scope, function(
            event, msg) {
          console.log('msg received' + msg.type);
          if (scope.userId === msg.userId) {
            scope.bookings.clear();            
          }            
        });
      }
    };
  }]);
  return mod;
});

