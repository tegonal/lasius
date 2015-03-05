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
