
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
        date:'='
      },
      link: function(scope, iElement, iAttrs) {
        
        var pattern = 'DDMMYYYYHHmmss';               
        
        var load = function(date) {
          var from = date.startOf('day').format(pattern);
          var to = date.endOf('day').format(pattern);
          
          bookingHistoryService.getTimeBookingHistory(scope.userId, from, to).then(function(bookings) {
            scope.bookings = bookings;          
          });
        };
        
        scope.diff = function(booking) {
          return moment.duration(booking.end).subtract(booking.start).asHours();
        };
        
        scope.$watch('date',
            function(value){
              load(value);                
            }, true);
        
        msgBus.onMsg('UserTimeBookingHistoryEntryAdded', scope, function(
            event, msg) {
          console.log('msg received' + msg.type);
          if (scope.userId === msg.booking.userId && scope.date.startOf('day').unix() === moment(msg.booking.start).startOf('day').unix()) {
            scope.bookings.push(msg.booking);
            scope.$apply();
          }          
        });
        
        msgBus.onMsg('UserTimeBookingHistoryEntryRemoved', scope, function(
            event, msg) {
          console.log('msg received' + msg.type);
          for(i=0;i<scope.bookings.length;i++){
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

