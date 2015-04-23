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

  var mod = angular.module('services.alert', []);
  mod.factory('alertService', function () {

	function checkAlerts(targetScope) {
    if(!targetScope.alerts) {
      targetScope.alerts = [];
    }
	}
    
  return {
    /**
      * Add an alert to the global scope using type (error, info, warning)
    */
    addAlert: function (targetScope, timeout, type, msg) {
      var message = {'type': type, 'msg': msg};

      checkAlerts(targetScope);

      targetScope.alerts.push(message);

      // If it's an info message, automatically remove the element after 1 second.
      if(type === 'info') {
        timeout(function() {
          var index = targetScope.alerts.indexOf(message);

          if(index > -1) {
            targetScope.alerts.splice(index, 1); 
          }
        }, 1000, true);
      }
    },

    removeAlert: function(targetScope) {
      return function(index) {
        checkAlerts(targetScope);
          targetScope.alerts.splice(index, 1);
        };
      }
    };
  });
  
  return mod;
});