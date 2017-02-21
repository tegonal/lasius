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
// `main.js` is the file that sbt-web will use as an entry point
(function (requirejs) {
  'use strict';
  
//attach the .equals method to Array's prototype to call it on any array
  Array.prototype.equals = function (array) {
      // if the other array is a falsy value, return
      if (!array)
          return false;

      // compare lengths - can save a lot of time 
      if (this.length != array.length)
          return false;

      for (var i = 0, l=this.length; i < l; i++) {
          // Check if we have nested arrays
          if (this[i] instanceof Array && array[i] instanceof Array) {
              // recurse into the nested arrays
              if (!this[i].equals(array[i]))
                  return false;       
          }           
          else if (this[i] != array[i]) { 
              // Warning - two different object instances will never be equal: {x:20} != {x:20}
              return false;   
          }           
      }       
      return true;
  };   

  // -- RequireJS config --
  requirejs.config({
    // Packages = top-level folders; loads a contained file named 'main.js"
    packages: [],
    shim: {
      'jsRoutes': {
        deps: [],
        // it's not a RequireJS module, so we have to tell it what var is returned
        exports: 'jsRoutes'
      },
      // Hopefully this all will not be necessary but can be fetched from WebJars in the future
      'angular': {
        deps: ['jquery'],
        exports: 'angular'
      },
      'angular-animate': ['angular'],
      'angular-route': ['angular'],
      'angular-cookies': ['angular'],      
      'bootstrap': ['jquery'],
      'angular-ui-bootstrap': ['bootstrap', 'angular'],
      'angular-translate': ['angular'],
      'angular-translate-loader-static-files': [ 'angular', 'angular-translate' ],
      'ng-grid': ['angular'],
      'bootstrap-select': ['angular', 'bootstrap'],
      'angular-sanitize': ['angular'],
      'ui.select': ['angular', 'bootstrap', 'bootstrap-select', 'angular-sanitize'],
      'moment': [],
      'moment-timezone': [],
      'angular-moment': ['angular', 'moment', 'moment-timezone'],
      'd3': { exports: 'd3' },
      'nvd3': {
        exports: 'nv',
        deps: ['d3']
      }, 
      'angular-nvd3' : ['angular', 'nvd3', 'd3'],
      'angular-datepicker': ['angular']
    },
    paths: {
      'requirejs': ['../lib/requirejs/require'],
      'jquery': ['../lib/jquery/jquery'],
      'angular': ['../lib/angularjs/angular'],
      'angular-animate': ['../lib/angularjs/angular-animate'],
      'angular-route': ['../lib/angularjs/angular-route'],
      'angular-cookies': ['../lib/angularjs/angular-cookies'],      
      'bootstrap': ['../lib/bootstrap/js/bootstrap'],
      'angular-ui-bootstrap': ['../lib/angular-ui-bootstrap/ui-bootstrap-tpls'],
      'jsRoutes': ['/jsroutes'],
      'angular-translate': ['../lib/angular-translate/angular-translate'],
      'angular-translate-loader-static-files': [ '../lib/angular-translate-loader-static-files/angular-translate-loader-static-files' ],      
      'bootstrap-select': ['../lib/bootstrap-select/js/bootstrap-select'],
      'angular-sanitize': ['../lib/angular-sanitize/angular-sanitize'],
      'ui.select': ['../lib/angular-ui-select/select'],
      'moment': ['../lib/momentjs/moment', '../lib/momentjs/locale/de'],
      'moment-timezone': ['../lib/moment-timezone/builds/moment-timezone-with-data.min'],
      'angular-moment': ['../lib/angular-moment/angular-moment'],
      'd3': ['../lib/d3js/d3'],
      'nvd3': ['../lib/nvd3/nv.d3'],
      'angular-nvd3': ['../ext/angular-nvd3'],
      'angular-datepicker': ['../lib/angular-datepicker/dist/angular-datepicker']
    }
  });

  requirejs.onError = function (err) {
    console.log(err);
    console.log(err.stack);
  };

  // Load the app. This is kept minimal so it doesn't need much updating.
  require(['angular', 'angular-cookies', 'angular-route', 'jquery', 'bootstrap', 
           'angular-ui-bootstrap', 'angular-sanitize', 'ui.select', 'angular-translate', 
           'angular-translate-loader-static-files', 'moment', 'angular-moment', 'angular-animate', 'angular-nvd3', 'angular-datepicker', './app'],
    function (angular) {
      angular.bootstrap(document, ['app']);
    }
  );
})(requirejs);
