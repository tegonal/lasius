var userRoles = {
    Guest: "Guest",
    FreeUser: "FreeUser"
};

define(['angular', 
        './routes',
        './filters',
        './directives/lasCurrentTimeBooking',
        './directives/lasBooking',
        './directives/lasBookingHistory',
        './directives/lasBookingPieChart',
        './directives/lasBookingBarChart',
        './directives/lasStatisticsBarChart',
        './directives/lasWorkviewPieChart',
        './directives/lasDateRange',
        './directives/lasFavorites',
        './directives/lasStopEvent',
        './directives/lasLogin',
        './directives/lasLoginDropdown',
        './services/helper', 
        './services/playRoutes',
        './services/currentTimeBooking',
        './services/messages',
        './services/booking',
        './services/bookingHistory',
        './services/bookingStatistics',
        './services/favorites',
        './services/user',
        './services/auth',
        './services/alert'],
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
                                         'directives.lasWorkviewPieChart',
                                         'directives.lasDateRange',
                                         'directives.lasFavorites',
                                         'directives.lasStopEvent',
                                         'directives.lasLogin',
                                         'directives.lasLoginDropdown',
                                         'services.helper', 
                                         'services.playRoutes', 
                                         'services.currentTimeBooking',
                                         'services.messages',
                                         'services.booking',
                                         'services.bookingHistory',
                                         'services.bookingStatistics',
                                         'services.favorites',
                                         'services.user',
                                         'services.auth',
                                         'services.alert']);
  
  //declare constants
  mod.constant("MY_CONFIG", {
    "DATE_PATTERN": "DDMMYYYYHHmm",
    "MILLIS_PER_HOUR": 1000*60*60
  });
  
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
  
  mod.config(['$provide', function($provide) {
    $provide.decorator('$exceptionHandler', ['$log', '$injector', 'alertService', function($log, $injector, alertService) {
      return function(exception, cause) {
        // using the injector to retrieve scope and timeout, otherwise circular dependency
        var $rootScope = $injector.get('$rootScope');
        var $timeout = $injector.get('$timeout');

        $rootScope.$removeAlert = alertService.removeAlert($rootScope);
        alertService.addAlert($rootScope, $timeout, 'error', exception.message);
        
        // log error default style
        $log.error.apply($log, arguments);
      };
    }]);
  }]);
  
  mod.factory('httpErrorInterceptor', ['$q', '$rootScope', '$timeout', 'alertService', function($q, $rootScope, $timeout, alertService) {
    return {
      response: function (response) {
        return response || $q.when(response);
      },
      responseError: function (rejection) {
        // internal server error
        var status = rejection.status || {};
        var data = rejection.data || {};
        $rootScope.$removeAlert = alertService.removeAlert($rootScope);
        alertService.addAlert($rootScope, $timeout, 'error', status + ': ' + data);

        return $q.reject(rejection);
      }
    };
  }]);

  mod.config(['$httpProvider', function ($httpProvider) {
    $httpProvider.interceptors.push('httpErrorInterceptor');
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