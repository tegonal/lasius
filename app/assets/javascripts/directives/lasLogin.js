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

  var mod = angular.module('directives.lasLogin', []);
  mod.directive('lasLogin', ['$location', 'userService', function($location, userService) {
    return {
      restrict: 'E',
      transclude: true,
      templateUrl: '/assets/directives/las-login-tmpl.html',
      scope:  {
      },
      link: function(scope, iElement, iAttrs) {
        scope.user = {
            email: '',
            password: ''
        };
        
        scope.login = function() {
          scope.message = undefined;
          userService.login(scope.user.email, scope.user.password).then(function(user) {       
            if (user !== undefined) {
              //TODO: maybe fetch requested url and redirect to it back
              $location.path('/');
            }
            else {
              scope.message = "Login failed, please enter correct email and password!"; 
            }
          });
        };
      }
    };
  }]);
  return mod;
});

