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

      var mod = angular.module('directives.lasBookingHistory', []);
      mod
          .directive(
              'lasBookingHistory',
              [
                  'MY_CONFIG',
                  'bookingHistoryService',
                  'bookingService',
                  'msgBus',
                  'moment',
                  function(MY_CONFIG, bookingHistoryService, bookingService,
                      msgBus, moment) {
                    return {
                      restrict : 'E',
                      transclude : true,
                      templateUrl : '/assets/directives/las-booking-history-tmpl.html',
                      scope : {
                        userId : '=',
                        range : '='
                      },
                      link : function(scope, iElement, iAttrs) {

                        var load = function(range) {
                          if (range === undefined || range.from === undefined) {
                            return;
                          }
                          var from = range.from.format(MY_CONFIG.DATE_PATTERN);
                          var to = range.to.format(MY_CONFIG.DATE_PATTERN);

                          bookingHistoryService.getTimeBookingHistory(from, to)
                              .then(function(bookings) {
                                scope.bookings = bookings;
                              });
                        };
                        
                        scope.bookingForm = {
                            start:'',
                            end: '',
                            changed: false
                        };

                        scope.totalDiff = function(booking) {
                          return moment.duration(booking.end).subtract(
                              booking.start).asHours();
                        };

                        scope.dayDiff = function(booking) {
                          var start = moment.max(scope.range.from
                              .startOf('day'), moment(booking.start));
                          var end = moment.min(scope.range.to.endOf('day'),
                              moment(booking.end));
                          return moment.duration(end).subtract(start).asHours();
                        };

                        scope.sameDay = function(booking) {
                          return moment(booking.start).startOf('day').unix() === moment(
                              booking.end).startOf('day').unix();
                        };

                        scope.dateFormat = function(booking) {
                          if (scope.sameDay(booking)) {
                            return 'H:m';
                          } else {
                            return 'd.M H:m';
                          }
                        };

                        var removeBooking = function(bookingId) {
                          for (var i = 0; i < scope.bookings.length; i++) {
                            if (scope.bookings[i].id === bookingId) {
                              scope.bookings.splice(i, 1);
                              scope.$apply();
                              break;
                            }
                          }
                        };

                        scope.removeTimeBooking = function(bookingId) {
                          bookingHistoryService.removeTimeBooking(bookingId)
                              .then(function() {
                                removeBooking(bookingId);
                              });
                        };

                        var startBooking = function(booking) {
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

                        var stopBooking = function() {
                          bookingService.stop(scope.booking.id).then(
                              function() {
                                scope.booking = undefined;
                              });
                        };

                        scope.startStop = function(favorite) {
                          if (scope.isActive(favorite)) {
                            stopBooking(favorite);
                          } else {
                            startBooking(favorite);
                          }
                        };

                        scope.editBookingStart = function(booking) {
                          scope.editedBooking = booking;
                          scope.bookingForm.start = moment(booking.start).second(0).millisecond(0).toDate();
                          scope.bookingForm.end = undefined;
                        };
                        
                        scope.editBookingEnd = function(booking) {
                          scope.editedBooking = booking;
                          scope.bookingForm.start = undefined;
                          scope.bookingForm.end = moment(booking.end).second(0).millisecond(0).toDate();
                        };
                        
                        scope.changeEditBooking = function() {
                          scope.bookingForm.changed = true;                          
                        };

                        scope.saveEditingBooking = function() {
                          if (scope.editedBooking && scope.bookingForm.changed) {
                            var booking = scope.editedBooking;
                            scope.editedBooking = undefined;                            
                            bookingHistoryService.editTimeBooking(booking).then(
                                function() {
                                  // maybe notify change
                                  scope.bookingForm.changed = false;
                                });
                          }
                        };

                        var isEquals = function(booking, favorite) {
                          if (booking === undefined || favorite === undefined) {
                            return false;
                          }
                          return booking.categoryId === favorite.categoryId &&
                              booking.projectId === favorite.projectId &&
                              booking.tags.equals(favorite.tags);
                        };

                        scope.isActive = function(favorite) {
                          return isEquals(scope.booking, favorite);
                        };

                        scope.$watch('range', function(value) {
                          load(value);
                        }, true);
                                                
                        scope.$watch('bookingForm.start', function(start) {
                          if (start !== '' && start !== undefined) {
                            var s = moment(start);
                            scope.editedBooking.start = moment(scope.editedBooking.start).hour(s.hour()).minute(s.minute()).valueOf();
                          }
                        }, false);
                        
                        scope.$watch('bookingForm.end', function(end) {
                          if (end !== '' && end !== undefined) {
                            var e = moment(end);
                            scope.editedBooking.end = moment(scope.editedBooking.end).hour(e.hour()).minute(e.minute()).valueOf();
                          }
                        }, false);

                        msgBus
                            .onMsg('UserTimeBookingHistoryEntryAdded', scope,
                                function(event, msg) {
                                  console.log('msg received' + msg.type);
                                  if (scope.userId === msg.booking.userId &&
                                      scope.range.from.unix() === moment(
                                          msg.booking.start).startOf('day')
                                          .unix()) {
                                    scope.bookings.push(msg.booking);
                                    scope.$apply();
                                  }
                                });

                        msgBus.onMsg('UserTimeBookingHistoryEntryRemoved',
                            scope, function(event, msg) {
                              console.log('msg received' + msg.type);
                              removeBooking(msg.bookingId);
                            });

                        msgBus.onMsg('UserTimeBookingHistoryEntryCleaned',
                            scope, function(event, msg) {
                              console.log('msg received' + msg.type);
                              if (scope.userId === msg.userId) {
                                scope.bookings.clear();
                              }
                            });
                        
                        msgBus.onMsg('UserTimeBookingHistoryEntryChanged',
                            scope, function(event, msg) {
                              console.log('msg received' + msg.type);
                              if (scope.userId === msg.booking.userId) {
                                
                                var arrayLength = scope.bookings.length;
                                for (var i = 0; i < arrayLength; i++) {            
                                  if(scope.bookings[i].id === msg.booking.id) {
                                    scope.bookings[i] = msg.booking;
                                    break;
                                  }
                                }
                                
                                scope.$apply();
                              }
                            });

                        msgBus.onMsg('CurrentUserTimeBooking', scope, function(
                            event, msg) {
                          scope.booking = msg.booking;
                          scope.$apply();
                        });
                      }
                    };
                  } ]);
      return mod;
    });
