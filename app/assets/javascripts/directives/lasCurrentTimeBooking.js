
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
          console.log(msg);
          scope.$apply();           
        });
        
        }
    };
  }]);
  return mod;
});

