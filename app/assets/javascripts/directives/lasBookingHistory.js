
define(['angular'], function(angular) {
  'use strict';

  var mod = angular.module('directives.lasBookingHistory', []);
  mod.directive('lasBookingHistory', ['bookingHistoryService', 'bookingService', 'msgBus', 'moment', function(bookingHistoryService, bookingService, msgBus, moment) {
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
        
        var removeBooking = function(bookingId) {
          for(var i=0;i<scope.bookings.length;i++){
            if(scope.bookings[i].id === bookingId){
              scope.bookings.splice(i, 1);
              scope.$apply();
              break;
            }
          }   
        };
        
        scope.removeTimeBooking = function(bookingId) {
          bookingHistoryService.removeTimeBooking(scope.userId, bookingId).then(function(result){
            removeBooking(bookingId);
          });
        };
        
        var startBooking = function(booking) {
          bookingService.start(scope.userId, booking.categoryId, booking.projectId, booking.tags).then(function(result) {
            //assign dummy booking that row gets selected directly
            scope.booking = {
                projectId: booking.projectId,
                categoryId: booking.categoryId,
                tags: booking.tags
            };
          });
        };
        
        var stopBooking = function() {
          bookingService.stop(scope.userId, scope.booking.id).then(function() {
            scope.booking = undefined;
          });
        };
        
        scope.startStop = function(favorite) {
          if (scope.isActive(favorite)) {
            stopBooking(favorite);
          }
          else {
            startBooking(favorite);
          }
        };
        
        var isEquals = function(booking, favorite) {
          if (booking === undefined || favorite === undefined) {
            return false;
          }
          return booking.categoryId === favorite.categoryId &&
            booking.projectId === favorite.projectId &&
            booking.tags.equals(favorite.tags);
        };
        
        scope.isActive = function(favorite) {
          return isEquals(scope.booking, favorite);          
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
          removeBooking(msg.bookingId);             
        });
        
        msgBus.onMsg('UserTimeBookingHistoryEntryCleaned', scope, function(
            event, msg) {
          console.log('msg received' + msg.type);
          if (scope.userId === msg.userId) {
            scope.bookings.clear();            
          }            
        });
        
        msgBus.onMsg('CurrentUserTimeBooking', scope, function(
            event, msg) {
          scope.booking = msg.booking;
          scope.$apply();
        });
      }
    };
  }]);
  return mod;
});

