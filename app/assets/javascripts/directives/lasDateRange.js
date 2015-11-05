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

  var mod = angular.module('directives.lasDateRange', []);
  mod.directive('lasDateRange', ['moment', function(moment) {
    return {
      restrict: 'E',
      transclude: true,
      templateUrl: '/assets/directives/las-date-range-tmpl.html',
      scope:  {
        range: '=',
        selection: '=*?'
      },
      link: function(scope, iElement, iAttrs) {
        scope.date = moment();
        
        scope.selection = 'day';
        
        scope.isCollapsed = false;
        
        scope.dateChanging = false;
        
        scope.toggleSelection = function(mode) {
          scope.selection = mode;
          
          dateChanged(scope.dateSelection.date);
        };
        
        scope.dateSelection = {
            date: '',
            range: {
              from: '',
              to: ''
            }
        };
        
        scope.collapse = function() {
          scope.isCollapsed = !scope.isCollapsed;
        };
        
        scope.minus = function() {
          if (scope.selection === 'range') {
            var diff = scope.range.to.diff(scope.range.from);
            scope.range = {
                from: scope.range.from.subtract(diff),
                to: scope.range.to.subtract(diff).subtract(1, 'milliseconds')
            };
            scope.date = scope.range.from;
          }
          else {
            scope.date = scope.date.subtract(1, scope.selection+'s');
          }
        };
        scope.plus = function() {
          if (scope.selection === 'range') {
            var diff = scope.range.to.diff(scope.range.from);
            scope.range = {
                from: scope.range.from.add(diff).add(1, 'milliseconds'),
                to: scope.range.to.add(diff)
            };
            scope.date = scope.range.from;
          }
          else {
            scope.date = scope.date.add(1, scope.selection+'s');
          }
        };
        
        scope.$watch('date',
            function(value){
        //adjust dateSelection
            scope.dateChanging = true;
            try {
              if (scope.selection !== 'range') {
                scope.range = {
                    from: moment(value).startOf(scope.selection),
                    to: moment(value).endOf(scope.selection)
                };
                
                scope.dateSelection.date = value.toDate();
              }
              else {
                scope.dateSelection.range.from = scope.range.from.toDate();
                scope.dateSelection.range.to = scope.range.to.toDate();
              }
            }
            finally {
              scope.dateChanging = false;
            }
                                   
            
            }, true);
        
        var dateChanged = function(value) {
          if (!scope.dateChanging) {
            if (scope.selection === 'range') {
              scope.range = {
                  from: moment(scope.dateSelection.range.from).startOf('day'),
                  to: moment(scope.dateSelection.range.to).endOf('day')
              };
            }
            else {
              scope.date = moment(value.date);
            }                            
          }
        };
        
        scope.$watch('dateSelection',
            function(value){
              dateChanged(value);
            }, true); 
      }
    };
  }]);
  return mod;
});

