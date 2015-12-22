/*   __                          __                                          *\
*   / /____ ___ ____  ___  ___ _/ /       lasius                      *
*  / __/ -_) _ `/ _ \/ _ \/ _ `/ /        contributed by tegonal              *
*  \__/\__/\_, /\___/_//_/\_,_/_/         http://tegonal.com/                 *
*         /___/                                                               *
*                                                                             *
* This program is free software: you can redistribute it and/or modify it     *
* under the terms of the GNU General Public License as published by    *
* the Free Software Foundation, either version 3 of the License,              *
* or (at your option) any later version.                                      *
*                                                                             *
* This program is distributed in the hope that it will be useful, but         *
* WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY  *
* or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for *
* more details.                                                               *
*                                                                             *
* You should have received a copy of the GNU General Public License along     *
* with this program. If not, see http://www.gnu.org/licenses/                 *
*                                                                             *
\*                                                                           */
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

        var getChartOptions = function(index){
         return {
            chart: {
                type: 'pieChart',
                height: 250+index*30,
                width:250+index*30,
                x: function(d){return d.label;},
                y: function(d){return d.value;},               
                showLabels: false,
                showLegend: false,
                donutRatio: 0.65,
                donut:true,
                duration: 500,              
                labelType: 'percent',
                tooltips:true,
                margin: margin(index, 30),
                pie: {
                  valueFormat: function(n) {
                    var time = (n / MY_CONFIG.MILLIS_PER_HOUR).toFixed(1); 
                    return time + ' hours';
                  }
                },
                legend: {
                    margin: {
                        top: 0,
                        right: 0,
                        bottom: 0,
                        left: 0
                    }
                }
            }
         };
        };
        
        var pattern = 'DDMMYYYYHHmmss';
        
        var margin = function(index, offset) {
          return {
            left:-index*offset,
            top:-index*offset,
            bottom:0,
            right:0
          };                   
        };
        
        var limit = moment.duration(8, 'hours').asMilliseconds();
        scope.charts = [];
        scope.chartOptions = [];
        
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
          //recalculate missing chart options
          for (var i = scope.chartOptions.length; i < numberOfCharts; i++) { 
            scope.chartOptions.push(getChartOptions(i));
          }
          
          if (scope.charts.length > numberOfCharts) {
            scope.charts = scope.charts.slice(0, numberOfCharts);
          }
          
          //push 100% workload charts
          for (i = scope.charts.length; i < numberOfCharts-1; i++) { 
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
        
        msgBus.onMsg('CurrentUserTimeBookingEvent', scope, function(
            event, msg) {
          
          scope.booking = msg.booking.booking;
          scope.result = msg.booking;   
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

