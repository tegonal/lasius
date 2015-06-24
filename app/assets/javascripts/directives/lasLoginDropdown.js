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

  var mod = angular.module('directives.lasLoginDropdown', []);
  mod.directive('lasLoginDropdown', ['$location', 'Auth', 'userService', 'msgBus',function($location, Auth, userService, msgBus) {
    return {
      restrict: 'E',
      transclude: true,
      templateUrl: '/assets/directives/las-login-dropdown-tmpl.html',      
      scope: {
        
      },
      link: function(scope, iElement, iAttrs) {

        // Wrap the current user from the service in a watch expression
        scope.isLoggedIn = false;
        scope.$watch(function() {
          return userService.getUser();
        }, function(user) {
          if (angular.isDefined(user)) {
            scope.user = user;
            scope.isLoggedIn = Auth.isUserLoggedIn(scope.user);
          }
        }, true);
        
        scope.logout = function() {
           userService.logout();
        };
        
        scope.loggedOut = function() {
          userService.loggedOut();
          scope.user = undefined;
          $location.path('/login');          
        };
        
        msgBus.onMsg('UserLoggedOut', scope, function(event, msg) {
          console.log('msg received' + msg.type);  
            if(scope.isLoggedIn && userService.getUser().id == msg.userId) {
              scope.loggedOut();
              scope.$apply();
            }
          });
      }
    };
  }]);
  return mod;
});

