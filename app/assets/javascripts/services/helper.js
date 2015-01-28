/** Common helpers */
define(['angular'], function(angular) {
  'use strict';

  var mod = angular.module('services.helper', []);
  mod.service('helper', function() {
    return {
      sayHi: function() {
        return 'hi';
      }
    };
  });
  return mod;
});
