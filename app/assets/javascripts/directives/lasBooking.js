
define(['angular'], function(angular) {
  'use strict';

  var mod = angular.module('directives.lasBooking', []);
  mod.directive('lasBooking', ['structureService', function(structureService) {
    return {
      restrict: 'E',
      transclude: true,
      templateUrl: '/assets/directives/las-booking-tmpl.html',
      scope:  {
        userId:'='
      },
      link: function(scope, iElement, iAttrs) {
        //noting to do here
        structureService.getCategories(scope.userId).then(function(projects) {
          scope.projects = projects;
          scope.project = {};
        });
      }
    };
  }]);
  return mod;
});

