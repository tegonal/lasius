
define(['angular'], function(angular) {
  'use strict';

  var mod = angular.module('directives.lasWorkviewPieChart', []);
  mod.directive('lasWorkviewPieChart', ['$window', 'MY_CONFIG', 'currentTimeBookingService', 'msgBus', 'moment', function($window, MY_CONFIG, currentTimeBookingService, msgBus, moment) {
    return {
      restrict: 'E',
      templateUrl: '/assets/directives/las-workview-pie-chart-tmpl.html',
      scope:  {
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
        
        scope.toolTipContentFunction = function(){
          return function(key, x, y, e, graph) {
              //transfer into a readable format
              var time = (y.value / MY_CONFIG.MILLIS_PER_HOUR).toFixed(1); 
              return  '<h3>' + key + '</h3>' +
                    '<p>' + time + ' hours</p>';
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
        
        var updateCharts = function() {          
          var numberOfCharts = Math.ceil(scope.result.totalByDay / limit);
          
          //calculate percentage
          scope.progress = scope.result.totalByDay / limit;
                    
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
            scope.charts.push(fullChartData.slice(0));
          }
          //set chart-2 to 100%
          if (scope.charts.length > 1 && scope.charts[numberOfCharts-2][0].value != 100) {
            scope.charts[numberOfCharts-2] = fullChartData.slice(0);              
          }
          
          //push rest
          var rest = scope.result.totalByDay - (numberOfCharts-1)*limit;
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
        
        
        function cancelTimer() {
          if (activeTimeout) {
            $window.clearTimeout(activeTimeout);
            activeTimeout = null;
          }
        }
        
        function updateTime(momentInstance, apply) {

          // update every minute
          var millis = 60000;
          scope.result.totalByDay += moment.duration(millis, 'milliseconds').asMilliseconds();          
          updateCharts();   
          
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
          scope.result = msg;   
          if (scope.booking === undefined) {
            cancelTimer();
          }
          else {
            //add current duration
            scope.result.totalByDay +=  moment().diff(msg.booking.start);   
          }
                                            
          updateCharts();         
          
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
        
        currentTimeBookingService.getCurrentTimeBooking();
      }
    };
  }]);
  return mod;
});

