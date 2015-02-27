
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
        
        scope.isActive = function(favorite) {
          if (scope.booking === undefined) {
            return false;
          }
          return scope.booking.categoryId === favorite.categoryId &&
            scope.booking.projectId === favorite.projectId &&
            scope.booking.tags.equals(favorite.tags);
        };
        
        msgBus.onMsg('CurrentUserTimeBooking', scope, function(
            event, msg) {
          scope.booking = msg.booking;
          scope.$apply();
        });
      }
    };
  }]);
  return mod;
});

