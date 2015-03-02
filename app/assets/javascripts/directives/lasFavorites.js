
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
        

        var isEquals = function(booking, favorite) {
          if (booking === undefined || favorite === undefined) {
            return false;
          }
          return booking.categoryId === favorite.categoryId &&
            booking.projectId === favorite.projectId &&
            booking.tags.equals(favorite.tags);
        };
        
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
          return isEquals(scope.booking, favorite);          
        };
        
        msgBus.onMsg('CurrentUserTimeBooking', scope, function(
            event, msg) {
          scope.booking = msg.booking;
          scope.$apply();
        });
                
        msgBus.onMsg('FavoriteAdded', scope, function(
            event, msg) {
          if (msg.userId === scope.userId) {
            scope.favorites.favorites.push(msg.bookingStub);
          
            scope.$apply();
          }
        });
        
        msgBus.onMsg('FavoriteRemoved', scope, function(
            event, msg) {
          if (msg.userId === scope.userId) {
            for(var i=0;i<scope.favorites.favorites.length;i++){
              if(isEquals(scope.favorites.favorites[i], msg.bookingStub)){
                scope.favorites.favorites.splice(i, 1);
                scope.$apply();
                return;
              }
            }
          }
        });
      }
    };
  }]);
  return mod;
});

