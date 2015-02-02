define(['angular', 
        './routes',
        './filters',
        './directives/ngCurrentTimeBooking',
        './directives/ngBooking',
        './services/helper', 
        './services/playRoutes',
        './services/currentTimeBooking'],
    function(angular) {
  'use strict';

  var mod = angular.module('app', ['ngRoute', 
                                   'ui.bootstrap',
                                    'routes',
                                         'filters',
                                         'directives.ngCurrentTimeBooking',
                                         'directives.ngBooking',
                                         'services.helper', 
                                         'services.playRoutes', 
                                         'services.currentTimeBooking']);
    
  return mod;
});