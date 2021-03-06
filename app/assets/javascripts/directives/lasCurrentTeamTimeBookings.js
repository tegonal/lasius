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
define(
    [ 'angular' ],
    function(angular) {
      'use strict';

      var mod = angular.module('directives.lasCurrentTeamTimeBookings', []);
      mod
          .directive(
              'lasCurrentTeamTimeBookings',
              [
                  '$log',
                  'MY_CONFIG',
                  'bookingService', 
                  'currentTimeBookingService',                  
                  'msgBus',
                  'moment',
                  function($log, MY_CONFIG, bookingService, currentTimeBookingService, msgBus, moment) {
                    return {
                      restrict : 'E',
                      transclude : true,
                      replace: true,
                      templateUrl : '/assets/directives/las-current-team-time-bookings-tmpl.html',
                      scope : {     
                        user : '='
                      },
                      link : function(scope, iElement, iAttrs) {                        
                        scope.$watch('team', function(team) {
                          currentTimeBookingService.getTeamTimeBooking(team.id.$oid).then(function(response) {
                            scope.bookings = response;
                          });                            
                        }, false);
                        
                        if (scope.user.teams && scope.user.teams.length > 0) {
                          scope.team = scope.user.teams[0];
                        }
                        
                        msgBus.onMsg('CurrentTeamTimeBookings', scope, function(
                            event, msg) {
                          if (scope.team && msg.teamId.$oid === scope.team.id.$oid) {
                            //update
                            scope.bookings = msg;
                            scope.$apply();
                          }
                        });
                        
                        var isEquals = function(booking, bookingStub) {
                          if (booking === undefined || bookingStub === undefined) {
                            return false;
                          }
                          return booking.categoryId === bookingStub.categoryId &&
                            booking.projectId === bookingStub.projectId &&
                            booking.tags.equals(bookingStub.tags);
                        };
                        
                        scope.isSame = function(bookingStub) {
                          return isEquals(scope.booking, bookingStub);          
                        };
                        
                        scope.startBooking = function(booking) {
                          bookingService.start(booking.categoryId,
                              booking.projectId, booking.tags).then(function() {
                            // assign dummy booking that row gets selected
                            // directly
                            scope.booking = {
                              projectId : booking.projectId,
                              categoryId : booking.categoryId,
                              tags : booking.tags
                            };
                          });
                        };
                        
                        scope.$watch(currentTimeBookingService.getCurrentTimeBooking, function(value) {
                          if (value) {
                            scope.booking = value.booking;
                          }                          
                        });
                      }
                    };
                  } ]);
      return mod;
    });
