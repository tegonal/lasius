
define(['angular'], function(angular) {
  'use strict';

  var mod = angular.module('directives.ngBooking', []);
  mod.directive('ngBooking', function() {
    return {
      restrict: 'E',
      transclude: true,
      templateUrl: '/assets/directives/ng-booking-tmpl.html',
      scope:  {
        bookingService: '='
      },
      link: function(scope, iElement, iAttrs) {
        //noting to do here
      }
    };
  });
  return mod;
});

