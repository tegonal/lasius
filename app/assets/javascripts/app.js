define(['angular', 
        './routes',
        './filters',
        './directives/lasCurrentTimeBooking',
        './directives/lasBooking',
        './services/helper', 
        './services/playRoutes',
        './services/currentTimeBooking',
        './services/messages',
        './services/structure'],
    function(angular) {
  'use strict';

  var mod = angular.module('app', ['ngRoute', 
                                   'ui.bootstrap',
                                    'routes',
                                         'filters',
                                         'directives.lasCurrentTimeBooking',
                                         'directives.lasBooking',
                                         'services.helper', 
                                         'services.playRoutes', 
                                         'services.currentTimeBooking',
                                         'services.messages',
                                         'services.structure']);
  
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
  
  return mod;
});