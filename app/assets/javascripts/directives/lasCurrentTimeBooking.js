
define(['angular'], function(angular) {
  'use strict';

  var mod = angular.module('directives.lasCurrentTimeBooking', []);
  mod.directive('lasCurrentTimeBooking', ['currentTimeBookingService', 'msgBus', function(currentTimeBookingService, msgBus) {
    return {
      restrict: 'E',
      transclude: true,
      templateUrl: '/assets/directives/las-current-time-booking-tmpl.html',
      scope:  {
        userId: '='
      },
      link: function(scope, iElement, iAttrs) {
        currentTimeBookingService.getCurrentTimeBooking(scope.userId);
        
        msgBus.onMsg('CurrentUserTimeBooking', scope, function(event, msg) {
          console.log('msg received' + msg.type);
          scope.booking = msg.booking;
          
          scope.disableButton = scope.booking === undefined;
          
          console.log(msg);
          scope.$apply();           
        });
        
        scope.stopBooking = function(bookingId) {
          currentTimeBookingService.stopBooking(scope.userId, bookingId).then(function() {
            scope.disableButton = true;
            //TODO: already remove
          });
        };
        
        }
    };
  }]);
  return mod;
});

