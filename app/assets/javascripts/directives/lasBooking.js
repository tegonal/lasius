
define(['angular'], function(angular) {
  'use strict';

  var mod = angular.module('directives.lasBooking', []);
  mod.directive('lasBooking', ['bookingService', function(bookingService) {
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
        bookingService.getCategories(scope.userId).then(function(projects) {
          scope.projects = projects;          
        });
        
        scope.projectSelectionChanged = function() {
           scope.availableTags = scope.project.selected.project.tags;
           scope.tags = {};
        };
        
        scope.newTag = function(name) {
          return {id: name};          
        };
        
        scope.start = function() {
          var tagStrings = [];
          angular.forEach(scope.tags.selected, function(value, key) {
            this.push(value.id);
          }, tagStrings);
          
          bookingService.start(scope.userId, scope.project.selected.categoryId, scope.project.selected.project.id, tagStrings).then(function(result) {
            //reset current selection
            scope.project = {};
            scope.tags = {};          
          });
        };
      }
    };
  }]);
  return mod;
});

