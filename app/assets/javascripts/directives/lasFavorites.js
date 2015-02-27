
define(['angular'], function(angular) {
  'use strict';

  var mod = angular.module('directives.lasFavorites', []);
  mod.directive('lasFavorites', ['favoritesService', 'msgBus', 'moment', function(favoritesService, msgBus, moment) {
    return {
      restrict: 'E',
      transclude: true,
      templateUrl: '/assets/directives/las-favorites-tmpl.html',
      scope:  {
        userId:'='
      },
      link: function(scope, iElement, iAttrs) {
        
        favoritesService.getFavorites(scope.userId).then(function(favorites) {
          scope.favorites = favorites;
        });
        
        scope.removeFavorite = function(categoryId, projectId, tags) {
          favoritesService.removeFavorite(scope.userId, categoryId, projectId, tags).then(function(favorites) {
            scope.favorites = favorites;
          });
        };
      }
    };
  }]);
  return mod;
});

