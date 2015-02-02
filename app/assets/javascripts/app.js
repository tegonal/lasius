define(['angular', 
        './routes',
        './filters',
        './directives/ngCurrentTimeBooking',
        './directives/ngBooking',
        './services/helper', 
        './services/playRoutes'],
    function(angular) {
  'use strict';

  var mod = angular.module('app', ['ngRoute', 
                                    'routes',
                                         'filters',
                                         'directives.ngCurrentTimeBooking',
                                         'directives.ngBooking',
                                         'services.helper', 
                                         'services.playRoutes']);
    
  return mod;
});