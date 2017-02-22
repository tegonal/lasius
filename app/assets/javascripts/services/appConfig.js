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

  var mod = angular.module('services.appConfig', []);
  mod.factory('appConfigService', ['$q', 'playRoutes', '$log', function ($q, playRoutes,$log) {
    var config;
    
    return {
      resolveConfig: function() {
        var deferred = $q.defer();        
        if (config) {
          deferred.resolve(config);
        }
        else {
          $log.info('Load application config...');
          playRoutes.controllers.ApplicationController.config().get().then(function (response) {
              config = response.data;              
              deferred.resolve(config);
            });            
        }
        
        return deferred.promise;
      },
      getConfig: function () {
        return config;        
      }
    };
  }]);
  
  return mod;
});
