/**
 * Current Time Booking service
 */
define(['angular'], function (angular) {
  'use strict';

  var mod = angular.module('services.favorites', []);
  mod.factory('favoritesService', ['$http', '$location', '$q', 'playRoutes', '$log', function ($http, $location, $q, playRoutes, $log) {
    
    return {             
      getFavorites: function () {
        return playRoutes.controllers.UserFavoritesController.getFavorites().get().then(function (response) {
          return response.data;          
        }, function(reason) {
          $log.debug("Failed loading document:"+reason);
          return reason.data;
        });
      },
      addFavorite: function(categoryId, projectId, tags) {
        return playRoutes.controllers.UserFavoritesController.addFavorite(categoryId, projectId, tags).post().then(function (response) {
          return response.data;          
        }, function(reason) {
          $log.debug("Failed loading document:"+reason);
          return reason.data;
        });
      },
      removeFavorite: function(categoryId, projectId, tags) {
        return playRoutes.controllers.UserFavoritesController.removeFavorite(categoryId, projectId, tags).delete().then(function (response) {
          return response.data;          
        }, function(reason) {
          $log.debug("Failed loading document:"+reason);
          return reason.data;
        });
      }
    };
  }]);
 
  return mod;
});
