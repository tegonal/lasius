
define(['angular'], function(angular) {
  'use strict';

  var mod = angular.module('directives.lasLogin', []);
  mod.directive('lasLogin', ['$location', 'userService', function($location, userService) {
    return {
      restrict: 'E',
      transclude: true,
      templateUrl: '/assets/directives/las-login-tmpl.html',
      scope:  {
      },
      link: function(scope, iElement, iAttrs) {
        scope.user = {
            email: '',
            password: ''
        };
        
        scope.login = function() {
          userService.login(scope.user.email, scope.user.password).then(function(user) {       
            if (user !== undefined) {
              //TODO: maybe fetch requested url and redirect to it back
              $location.path('/');
            }
            else {
              $location.path('/login_failed');
            }
          });
        };
      }
    };
  }]);
  return mod;
});

