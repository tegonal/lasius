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

  var mod = angular.module('directives.lasStatisticsBarChart', []);
  mod.directive('lasStatisticsBarChart', ['MY_CONFIG', 'bookingStatisticsService', 'msgBus', 'moment', function(MY_CONFIG, bookingStatisticsService, msgBus, moment) {
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
  
        scope.chartOptions = {
            chart: {
                type: 'multiBarChart',
                height: scope.height,
                width: scope.width,
                noData: 'No Statistics found!',
                margin : {
                    top: 50,
                    right: 50,
                    bottom: 50,
                    left: 50
                },
                showValues: true,
                showXAxis: true,
                showYAxis: true,
                showControls: false,
                clipEdge: true,
                reduceXTicks: true,
                tooltips:true,
                duration: 500,
                stacked: true,
                x: function (d) { return d.day; },
                y: function (d) { return d.duration; },
                tooltipContent: function(key, x, y, e, graph) {
                  return  '<h3>' + key + '</h3>' +
                  '<p>Date:<b>' + x +'</b><br />Hours:<b>' + y + '</b></p>';
                },
                staggerLabels: false,
                xAxis: {
                    axisLabel: 'Tag',
                    showMaxMin: false,
                    tickFormat: function(day) {
                      return moment(day).format("D.M.YY");            
                    },
                    rotateXLabel: true
                },
                yAxis: {
                    axisLabel: 'Stunden',
                    axisLabelDistance: -20,
                    showMaxMin: true,
                    tickFormat: function(d) {
                      var time = (d / MY_CONFIG.MILLIS_PER_HOUR).toFixed(1); 
                      return time;
                    }
                }
            }
        };
                       
        var load = function(range) {
          if (range === undefined || range.from === undefined) {
            return;
          }
          
          var pattern = 'DDMMYYYY0000';
          var from = range.from.format(pattern);
          var to = range.to.format(pattern);
                  
          bookingStatisticsService.getStatistics(scope.source, from, to).then(function(statistics) {
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

