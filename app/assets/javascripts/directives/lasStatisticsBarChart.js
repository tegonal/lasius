
define(['angular'], function(angular) {
  'use strict';

  var mod = angular.module('directives.lasStatisticsBarChart', []);
  mod.directive('lasStatisticsBarChart', ['bookingStatisticsService', 'msgBus', 'moment', function(bookingStatisticsService, msgBus, moment) {
    return {
      restrict: 'E',
      templateUrl: '/assets/directives/las-statistics-bar-chart-tmpl.html',
      scope:  {
        userId:'=',
        source:'=',
        range:'=',
        width: '=',
        height: '=',
      },
      link: function(scope, iElement, iAttrs) {
        
        scope.xFunction = function(){
          return function(d) {
              return d.day;
          };
        };
        scope.yFunction = function(){
          return function(d) { 
            return d.duration; 
          };
        };
        
        var millisPerHour = 1000*60*60;
        scope.yAxisFunction = function() {
          return function(d) {
            var time = (d / millisPerHour).toFixed(1); 
            return time;
          };
        };
        
        scope.xAxisFunction = function() {
          return function(day) {
            return moment(day).format("D");            
          };
        };
        
        scope.toolTipContentFunction = function(){
          return function(key, x, y, e, graph) {
              return  '<h1>' + key + '</h1>' +
                    '<p>' + y + ' hours</p>';
          };
        };               
                       
        var load = function(range) {
          if (range === undefined || range.from === undefined) {
            return;
          }
          
          var pattern = 'DDMMYYYY000000';
          var from = range.from.format(pattern);
          var to = range.to.format(pattern);
                  
          bookingStatisticsService.getStatistics(scope.source, scope.userId, from, to).then(function(statistics) {
            scope.statistics = statistics;
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

