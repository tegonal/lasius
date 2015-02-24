
define(['angular'], function(angular) {
  'use strict';

  var mod = angular.module('directives.lasDateRange', []);
  mod.directive('lasDateRange', ['moment', function(moment) {
    return {
      restrict: 'E',
      transclude: true,
      templateUrl: '/assets/directives/las-date-range-tmpl.html',
      scope:  {
        range: '=',
        selection: '='
      },
      link: function(scope, iElement, iAttrs) {
        scope.date = moment();
        
        scope.minus = function() {
          scope.date = scope.date.subtract(1, scope.selection);
        };
        scope.plus = function() {
          scope.date = scope.date.add(1, scope.selection);
        };
        
        scope.$watch('date',
            function(value){
              scope.range = {
                  from: moment(value).startOf(scope.selection),
                  to: moment(value).endOf(scope.selection)
              };
            }, true);
        
      }
    };
  }]);
  return mod;
});

