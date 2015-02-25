define(['angular', 
        './routes',
        './filters',
        './directives/lasCurrentTimeBooking',
        './directives/lasBooking',
        './directives/lasBookingHistory',
        './directives/lasBookingPieChart',
        './directives/lasBookingBarChart',
        './directives/lasStatisticsBarChart',
        './directives/lasBookingStackedAreaStats',
        './directives/lasDateRange',
        './services/helper', 
        './services/playRoutes',
        './services/currentTimeBooking',
        './services/messages',
        './services/booking',
        './services/bookingHistory',
        './services/bookingStatistics'],
    function(angular) {
  'use strict';
  
  require(['d3'], function(d3) {
    window.d3 = d3;
    require(['nvd3'], function(nvd3) {
      console.log(nvd3);
    });
  });

  var mod = angular.module('app', ['ngRoute',
                                   'ngSanitize',
                                   'nvd3ChartDirectives',
                                   'ngAnimate',
                                   'angularMoment',
                                   'ui.select',
                                   'ui.bootstrap',
                                    'routes',
                                         'filters',
                                         'directives.lasCurrentTimeBooking',
                                         'directives.lasBooking',
                                         'directives.lasBookingHistory',
                                         'directives.lasBookingPieChart',
                                         'directives.lasBookingBarChart',
                                         'directives.lasStatisticsBarChart',
                                         'directives.lasBookingStackedAreaStats',
                                         'directives.lasDateRange',
                                         'services.helper', 
                                         'services.playRoutes', 
                                         'services.currentTimeBooking',
                                         'services.messages',
                                         'services.booking',
                                         'services.bookingHistory',
                                         'services.bookingStatistics']);
  
  mod.factory('msgBus', ['$rootScope', function($rootScope) {
    var msgBus = {};
    msgBus.emitMsg = function(msg) {
      $rootScope.$emit(msg.type, msg);
    };
    msgBus.onMsg = function(msg, scope, func) {
      var unbind = $rootScope.$on(msg, func);
      scope.$on('$destroy', unbind);
    };
    return msgBus;
  }]);
  
  mod.run(['clientMessageService', function (clientMessageService) {
    console.log("Start clientMessageService");
    clientMessageService.start();
  }]);
  
  mod.run(function(amMoment) {
    amMoment.changeLocale('de');
  });
  
  mod.constant('angularMomentConfig', {
    preprocess: 'utc' // optional
  });
  
  return mod;
});