
define(['angular'], function(angular) {
  'use strict';

  var mod = angular.module('directives.ngCurrentTimeBooking', []);
  mod.directive('ngCurrentTimeBooking', function() {
    return {
      restrict: 'E',
      transclude: true,
      templateUrl: '/assets/directives/ng-current-time-booking-tmpl.html',
      scope:  {
        currentTimeBookingService: '=',
        userId: '='
      },
      link: function(scope, iElement, iAttrs) {
        scope.currentTimeBookingService.getCurrentTimeBooking(scope.userId).then(function(data) {
          scope.booking = data.booking;
        });
        
        }
    };
  });
  return mod;
});

