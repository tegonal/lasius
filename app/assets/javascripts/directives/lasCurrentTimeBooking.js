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
                        userId : '='
                      },
                      link : function(scope, iElement, iAttrs) {
                        var activeTimeout = null;
                        var currentValue;
                        var unwatchChanges;

                        scope.duration = {};
                        currentTimeBookingService
                            .getCurrentTimeBooking(scope.userId);

                        msgBus.onMsg('CurrentUserTimeBooking', scope, function(
                            event, msg) {
                          console.log('msg received' + msg.type);
                          scope.booking = msg.booking;

                          scope.noBooking = scope.booking === undefined;

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

                        function updateTime(momentInstance) {
                          scope.duration.moment = moment().subtract(momentInstance);
                          
                          scope.$apply();                          

                          // update every second
                          activeTimeout = $window.setTimeout(function() {
                            updateTime(momentInstance);
                          }, 1000);
                        }

                        function updateMoment() {
                          cancelTimer();
                          if (currentValue) {
                            var momentValue = moment.duration(currentValue);
                            updateTime(momentValue);
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
                  } ]);
      return mod;
    });
