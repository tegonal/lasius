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
            scope.chartOptions.chart.height = statistics.length * 40;
          });
        };
        
        scope.$watch('range',
            function(value){
              load(value);                              
            }, true);

        
        scope.chartOptions = {
            chart: {
              type: 'multiBarHorizontalChart',
              height: scope.height,
              margin : {top: 30, right: 20, bottom: 50, left: 150},
              x: function(d){return d.label;},
              y: function(d){return d.value;},
              showValues: true,
              showLegend: false,
              showXAxis: true,
              showYAxis: false,
              showControls: false,
              stacked:true,
              tooltips:true,
              noData:'No Statistics found!',              
              valueFormat:  function(d){
                  var time = (d / MY_CONFIG.MILLIS_PER_HOUR).toFixed(1); 
                    return time+' hours';                
              },
              tooltip: function (key, x, y, e, graph) {
                var time = (e.value / MY_CONFIG.MILLIS_PER_HOUR).toFixed(1); 
                return '<h3>' + x + '</h3>' +
                '<p>' +  time+' hours</p>';
              },
              xAxis: {
              duration: 500,
                  axisLabel: '',
                  showMaxMin: true
              }
          }
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

