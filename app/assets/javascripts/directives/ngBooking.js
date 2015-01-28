
define(['angular'], function(angular) {
  'use strict';

  var mod = angular.module('directives.ngBooking', []);
  mod.directive('ngBooking', function() {
    return {
      restrict: 'AE',
      transclude: true,
      templateUrl: '/assets/directives/ng-booking-tmpl.html',
      scope: true,
      controller: ['$scope', '$http', '$modal', function($scope, $http, $modal) {
        
      }],
      link: function(scope, iElement, iAttrs, ctrl) {
        //noting to do here
      }
    };
  });
  return mod;
});

