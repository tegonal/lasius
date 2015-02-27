
define(['angular'], function(angular) {
  'use strict';

  var mod = angular.module('directives.lasFavorites', []);
  mod.directive('lasFavorites', ['favoritesService', 'bookingService', 'msgBus', 'moment', function(favoritesService, bookingService, msgBus, moment) {
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
        
        var startBooking = function(favorite) {
          bookingService.start(scope.userId, favorite.categoryId, favorite.projectId, favorite.tags).then(function(result) {
            //assign dummy booking that row gets selected directly
            scope.booking = {
                projectId: favorite.projectId,
                categoryId: favorite.categoryId,
                tags: favorite.tags
            };
          });
        };
        
        var stopBooking = function() {
          bookingService.stop(scope.userId, scope.booking.id).then(function() {
            scope.booking = undefined;
          });
        };
        
        scope.startStop = function(favorite) {
          if (scope.isActive(favorite)) {
            stopBooking(favorite);
          }
          else {
            startBooking(favorite);
          }
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

