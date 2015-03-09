
define(['angular'], function(angular) {
  'use strict';

  var mod = angular.module('directives.lasBookingBarChart', []);
  mod.directive('lasBookingBarChart', ['MY_CONFIG', 'bookingStatisticsService', 'msgBus', 'moment', function(MY_CONFIG, bookingStatisticsService, msgBus, moment) {
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
    
        scope.valueFormatFunction = function(){
          return function(d){
            var time = (d / MY_CONFIG.MILLIS_PER_HOUR).toFixed(1); 
              return time+' hours';
          };
        };
                                         
        var load = function(range) {
          if (range === undefined || range.from === undefined) {
            return;
          }
          
          var from = range.from.format(MY_CONFIG.DATE_PATTERN);
          var to = range.to.format(MY_CONFIG.DATE_PATTERN);
                  
          bookingStatisticsService.getAggregatedStatistics(scope.source, from, to).then(function(statistics) {
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
        
        msgBus.onMsg('UserTimeBookingByCategoryEntryAdded', scope, function(
            event, msg) {
          if (scope.source==='category' && scope.userId === msg.booking.userId && scope.range.from.unix() === moment(msg.booking.start).startOf('day').unix()) {
            //TODO: implement specific operation to save performance
            load(scope.range);
            scope.$apply();
          }          
        });            
        
        msgBus.onMsg('UserTimeBookingByProjectEntryAdded', scope, function(
            event, msg) {
          if (scope.source==='project' && scope.userId === msg.booking.userId && scope.range.from.unix() === moment(msg.booking.start).startOf('day').unix()) {
          //TODO: implement specific operation to save performance
            load(scope.range);
            scope.$apply();
          }          
        });
        
        msgBus.onMsg('UserTimeBookingByTagEntryAdded', scope, function(
            event, msg) {
          if (scope.source==='tag' && scope.userId === msg.booking.userId && scope.range.from.unix() === moment(msg.booking.start).startOf('day').unix()) {
          //TODO: implement specific operation to save performance
            load(scope.range);
            scope.$apply();
          }          
        });
        
        msgBus.onMsg('UserTimeBookingByCategoryEntryRemoved', scope, function(
            event, msg) {
          if (scope.source==='category' && scope.userId === msg.booking.userId && scope.range.from.unix() === moment(msg.booking.start).startOf('day').unix()) {
            //TODO: implement specific operation to save performance
              load(scope.range);
              scope.$apply();
            }             
        });
        
        msgBus.onMsg('UserTimeBookingByProjectEntryRemoved', scope, function(
            event, msg) {
          if (scope.source==='project' && scope.userId === msg.booking.userId && scope.range.from.unix() === moment(msg.booking.start).startOf('day').unix()) {
            //TODO: implement specific operation to save performance
              load(scope.range);
              scope.$apply();
            }             
        });
        
        msgBus.onMsg('UserTimeBookingByTagEntryRemoved', scope, function(
            event, msg) {
          if (scope.source==='tag' && scope.userId === msg.booking.userId && scope.range.from.unix() === moment(msg.booking.start).startOf('day').unix()) {
            //TODO: implement specific operation to save performance
              load(scope.range);
              scope.$apply();
            }             
        });
      }
    };
  }]);
  return mod;
});

