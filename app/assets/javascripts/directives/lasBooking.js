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

  var mod = angular.module('directives.lasBooking', []);
  mod.directive('lasBooking', ['bookingService', 'msgBus', function(bookingService, msgBus) {
    return {
      restrict: 'E',
      transclude: true,
      templateUrl: '/assets/directives/las-booking-tmpl.html',
      scope:  {
      },
      link: function(scope, iElement, iAttrs) {
        
        scope.projects = [];
        scope.availableTags = [];
        scope.project = {};          
        scope.tags = {};
        
        //noting to do here
        bookingService.getCategories().then(function(projects) {
          scope.projects = projects;          
        });             
        
        scope.projectSelectionChanged = function() {
           scope.availableTags = scope.project.selected.tagCache;
           scope.tags = {};
        };
        
        scope.newTag = function(name) {
          return {id: name};          
        };
        
        scope.start = function() {
          var tagStrings = [];
          angular.forEach(scope.tags.selected, function(value, key) {
            this.push(value.id);
          }, tagStrings);
          
          bookingService.start(scope.project.selected.categoryId, scope.project.selected.project.id, tagStrings).then(function(result) {
            //reset current selection
            scope.project = {};
            scope.tags = {};          
          });
        };
        
        //listen on tag cache changes per project
        msgBus.onMsg('TagCacheChanged', scope, function(
            event, msg) {          
            
            var sortById = function(a, b){
              if(a.id < b.id) return -1;
              if(a.id > b.id) return 1;
              return 0;
            };
          
            for(var i=0;i<scope.projects.length;i++){
              if(scope.projects[i].project.id ===  msg.projectId){
                //remove tags
                for(var t=0;t<msg.removed.length;t++){
                  for(var t2=0;t2<scope.projects[i].tagCache.length;t2++){
                    if (msg.removed[t].id === scope.projects[i].tagCache[t2].id) {
                      scope.projects[i].tagCache.splice(t2, 1);
                      break;
                    }
                  }                  
                }
                
                //add new tags
                for(t=0;t<msg.added.length;t++){
                  scope.projects[i].tagCache.push(msg.added[t]);
                }
                
                //sort again                
                scope.projects[i].tagCache.sort(sortById);
                
                //check if current project is selected
                if (scope.project.selected.project.id === scope.projects[i].project.id) {
                  scope.availableTags = scope.projects[i].tagCache;
                }
                
                scope.$apply();
                return;
              }
            }            
          });
      }
    };
  }]);
  return mod;
});

