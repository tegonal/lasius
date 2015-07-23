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

  var mod = angular.module('directives.lasBookingsTable', []);
  mod.directive('lasBookingsTable', ['msgBus', 'bookingService', function(msgBus, bookingService) {
    return {
      restrict: 'E',
      templateUrl: '/assets/directives/las-bookings-table-tmpl.html',
      scope:  {
        userId: '=',
        bookings:'=',
        removeBooking:'&'
      },
      link: function(scope, iElement, iAttrs) {
        
        scope.hasRemoveBooking = angular.isDefined(iAttrs.removeBooking);
        
        var isEquals = function(booking, bookingStub) {
          if (booking === undefined || bookingStub === undefined) {
            return false;
          }
          return booking.categoryId === bookingStub.categoryId &&
            booking.projectId === bookingStub.projectId &&
            booking.tags.equals(bookingStub.tags);
        };
             
        var startBooking = function(bookingStub) {
          bookingService.start(bookingStub.categoryId, bookingStub.projectId, bookingStub.tags).then(function(result) {
            //assign dummy booking that row gets selected directly
            scope.booking = {
                projectId: bookingStub.projectId,
                categoryId: bookingStub.categoryId,
                tags: bookingStub.tags
            };
          });
        };
        
        var stopBooking = function() {
          bookingService.stop(scope.booking.id).then(function() {
            scope.booking = undefined;
          });
        };
        
        scope.startStop = function(bookingStub) {
          if (scope.isActive(bookingStub)) {
            stopBooking(bookingStub);
          }
          else {
            startBooking(bookingStub);
          }
        };        
        
        scope.isActive = function(bookingStub) {
          return isEquals(scope.booking, bookingStub);          
        };
        
        msgBus.onMsg('CurrentUserTimeBooking', scope, function(
            event, msg) {
          if (msg.userId == userId) {
            scope.booking = msg.booking;
            scope.$apply();
          }
        });
        
      }
    };
  }]);
  return mod;
});

