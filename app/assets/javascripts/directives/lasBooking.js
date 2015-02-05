
define(['angular'], function(angular) {
  'use strict';

  var mod = angular.module('directives.lasBooking', []);
  mod.directive('lasBooking', function() {
    return {
      restrict: 'E',
      transclude: true,
      templateUrl: '/assets/directives/las-booking-tmpl.html',
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

