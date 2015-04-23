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
