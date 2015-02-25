
define(['angular'], function(angular) {
  'use strict';

  var mod = angular.module('directives.lasBookingBarChart', []);
  mod.directive('lasBookingBarChart', ['bookingStatisticsService', 'msgBus', 'moment', function(bookingStatisticsService, msgBus, moment) {
    return {
      restrict: 'E',
      templateUrl: '/assets/directives/las-booking-bar-chart-tmpl.html',
      scope:  {
        userId:'=',
        source:'=',
        range:'=',
        serie: '=',
        width: '=',
        height: '=',
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
                       
        
        
        var load = function(range) {
          if (range === undefined || range.from === undefined) {
            return;
          }
          
          var pattern = 'DDMMYYYYHHmmss';
          var from = range.from.format(pattern);
          var to = range.to.format(pattern);
                  
          bookingStatisticsService.getAggregatedStatistics(scope.source, scope.userId, from, to).then(function(statistics) {
            scope.statistics = [{
              "key": scope.serie,
                "values": statistics
            }];
          });
        };
        
        scope.$watch('range',
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

