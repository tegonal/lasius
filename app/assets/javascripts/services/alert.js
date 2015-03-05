
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