
define(['angular'], function(angular) {
  'use strict';

  var mod = angular.module('directives.lasBookingTagStats', []);
  mod.directive('lasBookingTagStats', ['bookingStatisticsService', 'msgBus', 'moment', function(bookingStatisticsService, msgBus, moment) {
    return {
      restrict: 'E',
      transclude: true,
      templateUrl: '/assets/directives/las-booking-stats-tags-tmpl.html',
      scope:  {
        userId:'='
      },
      link: function(scope, iElement, iAttrs) {
        
        scope.date = moment();
        scope.xFunction = function(){
          return function(d) {
              return d.label;
          };
        };
        scope.yFunction = function(){
          return function(d) { 
            return d.value; 
          };
        };
        
        var pattern = 'DDMMYYYYHHmmss';        
        
        var load = function() {
          var from = scope.date.startOf('day').format(pattern);
          var to = scope.date.endOf('day').format(pattern);
          
          bookingStatisticsService.getAggregatedStatisticsByTagAndRange(scope.userId, from, to).then(function(statistics) {
            scope.statistics = statistics;
          });
        };
        load();
        
        scope.dayMinus = function() {
          scope.date = scope.date.subtract(1, 'day');
          load();
        };
        scope.dayPlus = function() {
          scope.date = scope.date.add(1, 'day');
          load();
        };        
      }
    };
  }]);
  return mod;
});

