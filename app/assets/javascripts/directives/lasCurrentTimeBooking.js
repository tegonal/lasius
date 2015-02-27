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
                  'currentTimeBookingService',
                  'msgBus',
                  'moment',
                  function($window,currentTimeBookingService, msgBus, moment) {
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

                        scope.duration = {};
                        scope.total_duration = {};
                        currentTimeBookingService
                            .getCurrentTimeBooking(scope.userId);
                        
                        scope.addToFavorites = function() {
                          currentTimeBookingService.addFavorite(scope.userId, scope.result.booking.categoryId, scope.result.booking.projectId, scope.result.booking.tags).then(function(favorites) {
                            scope.result.booking.isFavorite = true;
                          });
                        };
                        
                        scope.removeFromFavorites = function() {
                          currentTimeBookingService.removeFavorite(scope.userId, scope.result.booking.categoryId, scope.result.booking.projectId, scope.result.booking.tags).then(function(favorites) {
                            scope.result.booking.isFavorite = false;
                          });
                        };

                        msgBus.onMsg('CurrentUserTimeBooking', scope, function(
                            event, msg) {
                          console.log('msg received' + msg.type);
                          scope.result = msg;

                          scope.noBooking = scope.result.booking === undefined;

                          console.log(msg);
                          scope.$apply();
                        });

                        scope.stopBooking = function(bookingId) {
                          currentTimeBookingService.stopBooking(scope.userId,
                              bookingId).then(function() {
                            scope.noBooking = true;
                          });
                        };

                        function cancelTimer() {
                          if (activeTimeout) {
                            $window.clearTimeout(activeTimeout);
                            activeTimeout = null;
                          }
                        }

                        function updateTime(momentInstance, apply) {
                          scope.duration.moment = moment().subtract(momentInstance);
                          if (scope.result.totalBySameBooking) {
                            scope.total_duration.moment = moment(scope.duration.moment).add(moment.duration(scope.result.totalBySameBooking));
                          }
                          
                          if (apply) {
                            scope.$apply();
                          }

                          // update every second
                          activeTimeout = $window.setTimeout(function() {
                            updateTime(momentInstance, true);
                          }, 1000);
                        }

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
                      }
                    };
                  } ]);
      return mod;
    });
