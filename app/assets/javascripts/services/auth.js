/**
 * Current Time Booking service
 */
define(['angular'], function (angular) {
  'use strict';

  var mod = angular.module('services.auth', []);
  mod.factory('Auth', ['$http', '$location', '$q', 'playRoutes', '$log', 'userService', function ($http, $location, $q, playRoutes, $log, userService) {
    
    return {             
      login: function (email,  password) {
        return playRoutes.controllers.Application.login(email, password).post().then(function (response) {
          return response.data;          
        }, function(reason) {
          $log.debug("Failed loading document:"+reason);
          return reason.data;
        });
      },
      authorize: function(accessLevel) {
        return userService.resolveUser().then(function(user) {
          $log.debug('authorize:'+accessLevel+' => '+user.role);
          return accessLevel === undefined || accessLevel === userRoles.Guest || accessLevel === user.role;
        });            
      },
      isLoggedIn: function() {
        return userService.resolveUser().then(function(user) {
          return user.role !== userRoles.Guest;
        });           
      },
      isUserLoggedIn: function(user) {
        if (user === undefined) {
          return false;
        }
          return user.role !== userRoles.Guest;                  
      }
    };
  }]);
  
  /**
   * If the current route does not resolve, go back to the start page.
   */
  var checkAuth = function ($q, Auth, $rootScope, $location, $log) {
    $rootScope.$on("$routeChangeStart", function (event, next, current) {      
      return Auth.authorize(next.access).then(function(authorized) {
        $log.debug('check authorization:'+next.access+' -> '+authorized);
          if (!authorized) {
            Auth.isLoggedIn().then(function(loggedIn) {
              if(loggedIn) $location.path('/forbidden');
              else $location.path('/login');
            });
          }         
        });
      });
   };
  checkAuth.$inject = ['$q', 'Auth', '$rootScope', '$location', '$log'];
  mod.run(checkAuth);
 
  return mod;
});
