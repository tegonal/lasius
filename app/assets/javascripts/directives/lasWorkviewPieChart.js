
define(['angular'], function(angular) {
  'use strict';

  var mod = angular.module('directives.lasWorkviewPieChart', []);
  mod.directive('lasWorkviewPieChart', ['bookingStatisticsService', 'msgBus', 'moment', function(bookingStatisticsService, msgBus, moment) {
    return {
      restrict: 'E',
      templateUrl: '/assets/directives/las-workview-pie-chart-tmpl.html',
      scope:  {
        userId:'=',
        source:'=',
        range:'='
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
        
        var load = function(range) {
          if (range === undefined || range.from === undefined) {
            return;
          }
          var from = range.from.format(pattern);
          var to = range.to.format(pattern);
          
          bookingStatisticsService.getAggregatedStatistics(scope.source, scope.userId, from, to).then(function(statistics) {
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
        
        function cancelTimer() {
          if (activeTimeout) {
            $window.clearTimeout(activeTimeout);
            activeTimeout = null;
          }
        }
        
        function updateTime(momentInstance, apply) {
          scope.duration.moment = moment().subtract(momentInstance);
          
          if (apply) {
            scope.$apply();
          }

          // update every minute
          activeTimeout = $window.setTimeout(function() {
            updateTime(momentInstance, true);
          }, 60000);
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
        
        unwatchChanges = scope.$watch('booking.start',
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

