/*   __                          __                                          *\
*   / /____ ___ ____  ___  ___ _/ /       lasius                      *
*  / __/ -_) _ `/ _ \/ _ \/ _ `/ /        contributed by tegonal              *
*  \__/\__/\_, /\___/_//_/\_,_/_/         http://tegonal.com/                 *
*         /___/                                                               *
*                                                                             *
* This program is free software: you can redistribute it and/or modify it     *
* under the terms of the GNU General Public License as published by    *
* the Free Software Foundation, either version 3 of the License,              *
* or (at your option) any later version.                                      *
*                                                                             *
* This program is distributed in the hope that it will be useful, but         *
* WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY  *
* or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for *
* more details.                                                               *
*                                                                             *
* You should have received a copy of the GNU General Public License along     *
* with this program. If not, see http://www.gnu.org/licenses/                 *
*                                                                             *
\*                                                                           */
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
                
        
        favoritesService.getFavorites().then(function(favorites) {
          scope.favorites = favorites;
        });
        
        scope.removeFavorite = function(categoryId, projectId, tags) {
          favoritesService.removeFavorite(categoryId, projectId, tags).then(function(favorites) {
            scope.favorites = favorites;
          });
        };
                
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

