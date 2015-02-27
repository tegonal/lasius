
define(['angular'], function(angular) {
  'use strict';

  var mod = angular.module('directives.lasWorkviewPieChart', []);
  mod.directive('lasWorkviewPieChart', ['$window', 'bookingStatisticsService', 'msgBus', 'moment', function($window, bookingStatisticsService, msgBus, moment) {
    return {
      restrict: 'E',
      templateUrl: '/assets/directives/las-workview-pie-chart-tmpl.html',
      scope:  {
        userId:'='
      },
      link: function(scope, iElement, iAttrs) {
        
        var activeTimeout;
        var currentValue;
        var unwatchChanges;
        
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
        
        scope.margin = function(index, offset) {
          return {
            left:-index*offset,
            top:-index*offset,
            bottom:0,
            right:0
          };                   
        };
        
        var limit = moment.duration(8, 'hours').asMilliseconds();
        scope.charts = [];
        
        var generateCharts = function() {          
          var numberOfCharts = Math.ceil(scope.result.total / limit);
          var fullChartData = [
                               {
                                 label: "worked",
                                 value: 100
                               },
                               {
                                 label: "open",
                                 value: 0
                               }
                               ];
          
          //push 100% workload charts
          for (var i = scope.charts.length; i < numberOfCharts-1; i++) { 
            scope.charts.push(fullChartData);
          }
          //set chart-2 to 100%
          if (scope.charts[numberOfCharts-2][0].value != 100) {
            scope.charts[numberOfCharts-2] = fullChartData;              
          }
          
          //push rest
          var rest = scope.result.total - (numberOfCharts-1)*limit;
          var open = limit - rest;
          
          if (scope.charts.length < numberOfCharts) {
            //insert new
            scope.charts.push([
                               {
                                 label: "worked",
                                 value: rest
                               },
                               {
                                 label: "open",
                                 value: open
                               }
                               ]);
          }
          else {
            //update
            scope.charts[numberOfCharts-1] = [
                                              {
                                                label: "worked",
                                                value: rest
                                              },
                                              {
                                                label: "open",
                                                value: open
                                              }
                                              ];
          }
        };
        
        var load = function(date) {
          if (date === undefined) {
            return;
          }
          var from = date.format(pattern);
          var to = date.format(pattern);
          
          /*bookingStatisticsService.getAggregatedStatistics(scope.source, scope.userId, from, to).then(function(statistics) {
            scope.statistics = statistics;
          });*/
          
          scope.result = {
              total: moment.duration(10, 'hours').asMilliseconds(),
              booking: {
                start: moment.duration(1, 'hours').asMilliseconds()
              }
          };                            
          generateCharts();          
       };
       
       load(moment());
        
        function cancelTimer() {
          if (activeTimeout) {
            $window.clearTimeout(activeTimeout);
            activeTimeout = null;
          }
        }
        
        function updateTime(momentInstance, apply) {

          // update every minute
          var millis = 60000;
          scope.result.total += moment.duration(millis, 'milliseconds').asMilliseconds();          
          generateCharts();   
          
          if (apply) {
            scope.$apply();
          }
          
          activeTimeout = $window.setTimeout(function() {
            updateTime(momentInstance, true);
          }, millis);
        }
        
        msgBus.onMsg('CurrentUserTimeBooking', scope, function(
            event, msg) {
          
          scope.booking = msg.booking;
          if (scope.booking === undefined) {
            cancelTimer();
          }
          else {
            
          }
          
          console.log(msg);
          scope.$apply();
        });
        
        function updateMoment(apply) {
          cancelTimer();
          if (currentValue) {
            var momentValue = moment.duration(currentValue);
            updateTime(momentValue, false);
          }
        }
        
        unwatchChanges = scope.$watch('result.booking.start',
            function(value) {
              if ((typeof value === 'undefined') || (value === null) || (value === '')) {
                cancelTimer();
                if (currentValue) {
                  scope.duraction = null;
                  currentValue = null;
                }
                return;
              }

              currentValue = value;
              updateMoment();                              
            });
        
        scope.$on('$destroy', function() {
          cancelTimer();
        });
      }
    };
  }]);
  return mod;
});

