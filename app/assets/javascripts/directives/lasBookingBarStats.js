
define(['angular'], function(angular) {
  'use strict';

  var mod = angular.module('directives.lasBookingBarStats', []);
  mod.directive('lasBookingBarStats', ['bookingStatisticsService', 'msgBus', 'moment', function(bookingStatisticsService, msgBus, moment) {
    return {
      restrict: 'E',
      templateUrl: '/assets/directives/las-booking-bar-stats-tmpl.html',
      scope:  {
        userId:'=',
        source:'=',
        date:'=',
        serie: '='
      },
      link: function(scope, iElement, iAttrs) {
        
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
        
        var load = function(date) {
          if (date === undefined) {
            return;
          }
          var from = date.startOf('day').format(pattern);
          var to = date.endOf('day').format(pattern);
          
          bookingStatisticsService.getAggregatedStatistics(scope.source, scope.userId, from, to).then(function(statistics) {
            scope.statistics = [{
                "key": scope.serie,
                "values": statistics
            }];
          });
        };
        
        scope.$watch('date',
            function(value){
              load(value);                              
            }, true);
        
        scope.callbackFunction = function(){
          return function(chart){
              console.log('inner callback function', chart);
          };
        };
      }
    };
  }]);
  return mod;
});

