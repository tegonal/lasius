
define(['angular'], function(angular) {
  'use strict';

  var mod = angular.module('directives.lasStopEvent', []);
  mod.directive('lasStopEvent', [function() {
    return {
        restrict: 'A',
        link: function (scope, element, attr) {
            element.bind('click', function (e) {
                e.stopPropagation();
            });
        }
    };
 }]);
  return mod;
});