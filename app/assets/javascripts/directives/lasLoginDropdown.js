
define(['angular'], function(angular) {
  'use strict';

  var mod = angular.module('directives.lasLoginDropdown', []);
  mod.directive('lasLoginDropdown', ['$location', 'Auth', 'userService', 'msgBus',function($location, Auth, userService, msgBus) {
    return {
      restrict: 'E',
      transclude: true,
      templateUrl: '/assets/directives/las-login-dropdown-tmpl.html',      
      scope: {
        
      },
      link: function(scope, iElement, iAttrs) {

        // Wrap the current user from the service in a watch expression
        scope.isLoggedIn = false;
        scope.$watch(function() {
          return userService.getUser();
        }, function(user) {
          if (angular.isDefined(user)) {
            scope.user = user;
            scope.isLoggedIn = Auth.isUserLoggedIn(scope.user);
          }
        }, true);
        
        scope.logout = function() {
           userService.logout();
        };
        
        scope.loggedOut = function() {
          userService.loggedOut();
          scope.user = undefined;
          $location.path('/');          
        };
        
        msgBus.onMsg('UserLoggedOut', scope, function(event, msg) {
          console.log('msg received' + msg.type);  
            if(scope.isLoggedIn && userService.getUser().userId == msg.userId) {
              scope.loggedOut();
              scope.$apply();
            }
          });
      }
    };
  }]);
  return mod;
});

