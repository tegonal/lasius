define(['angular', 
        './routes',
        './filters',
        './directives/lasCurrentTimeBooking',
        './directives/lasBooking',
        './services/helper', 
        './services/playRoutes',
        './services/currentTimeBooking',
        './services/messages',
        './services/booking'],
    function(angular) {
  'use strict';

  var mod = angular.module('app', ['ngRoute',
                                   'ngSanitize',
                                   'angularMoment',
                                   'ui.select',
                                   'ui.bootstrap',
                                    'routes',
                                         'filters',
                                         'directives.lasCurrentTimeBooking',
                                         'directives.lasBooking',
                                         'services.helper', 
                                         'services.playRoutes', 
                                         'services.currentTimeBooking',
                                         'services.messages',
                                         'services.booking']);
  
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