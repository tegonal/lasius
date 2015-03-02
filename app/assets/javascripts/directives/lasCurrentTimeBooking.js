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
                  'favoritesService', 
                  'msgBus',
                  'moment',
                  function($window, currentTimeBookingService, favoritesService, msgBus, moment) {
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
                        currentTimeBookingService
                            .getCurrentTimeBooking(scope.userId);
                        
                        favoritesService.getFavorites(scope.userId).then(function(favorites) {
                          scope.favorites = favorites;
                          if (scope.result && scope.result.booking) {
                            scope.result.booking.isFavorite = isFavorite(scope.result.booking);
                          }
                        });
                        
                        scope.addToFavorites = function() {
                          favoritesService.addFavorite(scope.userId, scope.result.booking.categoryId, scope.result.booking.projectId, scope.result.booking.tags).then(function(favorites) {
                            scope.favorites = favorites;
                            scope.result.booking.isFavorite = isFavorite(scope.result.booking);
                          });
                        };
                        
                        scope.removeFromFavorites = function() {
                          favoritesService.removeFavorite(scope.userId, scope.result.booking.categoryId, scope.result.booking.projectId, scope.result.booking.tags).then(function(favorites) {
                            scope.favorites = favorites;
                            scope.result.booking.isFavorite = isFavorite( scope.result.booking);                            
                          });
                        };
                        
                       

                        msgBus.onMsg('CurrentUserTimeBooking', scope, function(
                            event, msg) {
                          scope.result = msg;

                          scope.noBooking = scope.result.booking === undefined;
                          if (scope.result.booking) {
                            scope.result.booking.isFavorite = isFavorite(scope.result.booking);
                          }

                          console.log(msg);
                          scope.$apply();
                        });
                        
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
