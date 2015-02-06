
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
        
        scope.projects = [];
        scope.availableTags = [];
        scope.project = {};          
        scope.tags = {};
        
        //noting to do here
        structureService.getCategories(scope.userId).then(function(projects) {
          scope.projects = projects;          
        });
        
        scope.projectSelectionChanged = function() {
           scope.availableTags = scope.project.selected.project.tags;
        };
        
        scope.newTag = function(name) {
          return {id: name};          
        };
      }
    };
  }]);
  return mod;
});

