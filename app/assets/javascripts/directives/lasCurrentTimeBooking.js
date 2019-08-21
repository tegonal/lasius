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

      var mod = angular.module('directives.lasCurrentTimeBooking', []);
      mod
          .directive(
              'lasCurrentTimeBooking',
              [
                  '$window',
                  '$uibModal',
                  '$log',
                  'MY_CONFIG',
                  'currentTimeBookingService',
                  'favoritesService', 
                  'msgBus',
                  'moment',
                  function($window, $uibModal, $log, MY_CONFIG, currentTimeBookingService, favoritesService, msgBus, moment) {
                    return {
                      restrict : 'E',
                      transclude : true,
                      templateUrl : '/assets/directives/las-current-time-booking-tmpl.html',
                      scope : {
                        userId : '=',
                        compact: '='
                      },
                      link : function(scope, iElement, iAttrs) {
                        var activeTimeout = null;
                        var currentValue;
                        var unwatchChanges;
                        
                        var isEquals = function(booking, favorite) {
                          if (booking === undefined || favorite === undefined) {
                            return false;
                          }
                          return booking.categoryId === favorite.categoryId &&
                            booking.projectId === favorite.projectId &&
                            booking.tags.equals(favorite.tags);
                        };
                        
                        var isFavorite = function(booking) {
                          if (booking === undefined || scope.favorites === undefined) {
                            return false;
                          }
                          var length = scope.favorites.favorites.length;
                          for (var i=0; i<length; ++i) {
                            var favorite = scope.favorites.favorites[i];
                            if (isEquals(booking, favorite)) {
                              return true;
                            }
                          }
                          return false;
                        };

                        scope.duration = {};
                        scope.total_duration = {};
//                        currentTimeBookingService
//                            .getCurrentTimeBooking().then(function(response) {
//                          handleCurrentUserTimeBooking(response.booking);
//                        });
                        currentTimeBookingService.resolveCurrentTimeBooking(true);
                        
                        favoritesService.getFavorites().then(function(favorites) {
                          scope.favorites = favorites;
                          if (scope.result && scope.result.booking) {
                            scope.result.booking.isFavorite = isFavorite(scope.result.booking);
                          }
                        });
                        
                        scope.addToFavorites = function() {
                          favoritesService.addFavorite(scope.result.booking.categoryId, scope.result.booking.projectId, scope.result.booking.tags).then(function(favorites) {
                            scope.favorites = favorites;
                            scope.result.booking.isFavorite = isFavorite(scope.result.booking);
                          });
                        };
                        
                        scope.removeFromFavorites = function() {
                          favoritesService.removeFavorite(scope.result.booking.categoryId, scope.result.booking.projectId, scope.result.booking.tags).then(function(favorites) {
                            scope.favorites = favorites;
                            scope.result.booking.isFavorite = isFavorite( scope.result.booking);                            
                          });
                        };
                        
                        var handleCurrentUserTimeBooking = function(msg) {
                          scope.result = msg;
                          scope.total_duration = {};

                          scope.noBooking = scope.result.booking === undefined;
                          scope.bookingPaused = scope.result.booking && scope.result.booking.end;
                          scope.bookingRunning = !scope.noBooking && !scope.bookingPaused;
                          if (scope.result.booking) {
                            scope.result.booking.isFavorite = isFavorite(scope.result.booking);
                          }

                          if (scope.result.booking) {
                            checkBooking(scope.result.booking.start);
                          }
                          
                          console.log(msg);
                        };
                        
                        msgBus.onMsg('FavoriteRemoved', scope, function(
                            event, msg) {
                          if (msg.userId === scope.userId) {
                            for(var i=0;i<scope.favorites.favorites.length;i++){
                              if(isEquals(scope.favorites.favorites[i], msg.bookingStub)){
                                scope.favorites.favorites.splice(i, 1);
                                scope.result.booking.isFavorite = isFavorite(scope.result.booking);                                
                                scope.$apply();
                                return;
                              }
                            } 
                          }
                        });

                        scope.stopBooking = function(bookingId) {
                          currentTimeBookingService.stopBooking(bookingId).then(function() {
                            scope.noBooking = true;
                          });
                        };

                        scope.pauseBooking = function(bookingId) {
                          currentTimeBookingService.pauseBooking(bookingId).then(function() {
                          });
                        };

                        scope.resumeBooking = function(bookingId) {
                          currentTimeBookingService.resumeBooking(bookingId).then(function() {
                            scope.noBooking = false;
                          });
                        };
                        
                        scope.changeStartTime = function(bookingId, time) {
                          var formatted = time.format(MY_CONFIG.FULL_DATE_PATTERN);
                          currentTimeBookingService.changeStartTime(bookingId, formatted);
                        };
                        
                        scope.showChangeStartTime = function() {
                          var modalInstance = $uibModal.open({
                            animation: true,
                            templateUrl: '/assets/dialogs/change-start-time.html',
                            controller: 'ChangeStartTimeCtrl',
                            size: 'sm',
                            resolve: {
                              time: function () {
                                return scope.result.booking.start;
                              }
                            }
                          });

                          modalInstance.result.then(function (time) {                           
                            scope.changeStartTime(scope.result.booking.id, time);
                          }, function () {
                            $log.info('Modal dismissed at: ' + new Date());
                          });
                        };
                        
                        function cancelTimer() {
                          if (activeTimeout) {
                            $window.clearTimeout(activeTimeout);
                            activeTimeout = null;
                          }
                        }

                        function updateTime(momentInstance, apply) {
                          scope.duration.moment = (scope.result.booking.end === undefined)?
                              momentInstance:
                              moment(scope.result.booking.end).subtract(momentInstance);

                          if (scope.result.totalBySameBooking) {
                            scope.total_duration.moment = moment(scope.duration.moment).add(moment.duration(scope.result.totalBySameBooking));
                          }
                          
                          if (apply) {
                            scope.$apply();
                          }

                          // update every second
                          if (scope.result.booking.end === undefined) {
                            activeTimeout = $window.setTimeout(function() {
                              updateTime(momentInstance.add(1, 's'), true);
                            }, 1000);
                          }
                        }

                        function updateMoment(apply) {
                          cancelTimer();
                          if (currentValue) {
                            var momentValue = moment.duration(moment().diff(moment(currentValue)));
                            
                            updateTime(momentValue, false);
                          }
                        }
                        
                        function checkBooking(value) {
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
                        }

                        unwatchChanges = scope.$watch('result.booking.start',
                            function(value) {
                              checkBooking(value);                              
                            });
                        
                        scope.$watch(currentTimeBookingService.getCurrentTimeBooking, function(value) {
                          if (value) {
                            handleCurrentUserTimeBooking(value);
                          }                          
                        });

                        scope.$on('$destroy', function() {
                          cancelTimer();
                        });
                      }
                    };
                  } ]);
      return mod;
    });
