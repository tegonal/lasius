/**
 * User service, exposes user model to the rest of the app.
 */
define(['angular'], function (angular) {
  'use strict';

  var mod = angular.module('services.user', ['ngCookies']);
  mod.factory('userService', ['$http', '$location', '$q', 'playRoutes', '$cookies', '$log', function ($http, $location, $q, playRoutes, $cookies, $log) {
    var user, token = $cookies['XSRF-TOKEN'];
    
    return {
      login: function (email, password) {
        return playRoutes.controllers.ApplicationController.login(email, password).post().then(function (response) {
          // return promise so we can chain easily
          token = response.data.token;
          return playRoutes.controllers.UsersController.authUser().get().then(function (response) {
            user = response.data;
            $log.info("Login succeeded:"+user);
            return user;          
          });
          }, function(reason) {
          $log.info("Login failed:"+reason.data);
          $location.replace('/login_failed');
          }
        );
      },
      logout: function () {
        return playRoutes.controllers.ApplicationController.logout().post().then(function () {
          $log.info("Sent goodbye");
        });
      },
      loggedOut: function () {
        // Logout on server in a real app
        delete $cookies['XSRF-TOKEN'];        
        token = undefined;
        user = undefined;
        $location.$$search = {}; // clear token & token signature
        $log.info("Good bye");        
      },
      resolveUser: function() {
        /* If the token is assigned, check that the token is still valid on the server */
        var deferred = $q.defer();        
        if (user) {
          deferred.resolve(user);
        }
        else if (token) {
          $log.info('Restoring user from cookie...');
          playRoutes.controllers.UsersController.authUser().get()
            .success(function (data) {
              $log.info('Welcome back, ' + data.docsafeUserId);
              user = data;
              deferred.resolve(user);
            })
            .error(function () {
              $log.info('Token no longer valid, please log in.');
              token = undefined;
              delete $cookies['XSRF-TOKEN'];          
              deferred.reject("Token invalid");
            });
        }
        else {
          user = {
              userId:'', role:'Guest'
          };
          deferred.resolve(user);
        }
        
        return deferred.promise;
      },
      getUser: function () {
        return user;        
      },
      getToken: function () {
        return token;        
      }      
    };
  }]);
  /**
   * Add this object to a route definition to only allow resolving the route if the user is
   * logged in. This also adds the contents of the objects as a dependency of the controller.
   */
  mod.constant('userResolve', {
    user: ['$q', 'userService', function ($q, userService) {
      var deferred = $q.defer();
      var user = userService.getUser();
      if (user) {
        deferred.resolve(user);
      } else {
        deferred.reject();
      }
      return deferred.promise;
    }]
  });
  /**
   * If the current route does not resolve, go back to the start page.
   */
  var handleRouteError = function ($rootScope, $location) {
    $rootScope.$on('$routeChangeError', function (/*e, next, current*/) {
      $location.path('/');
    });
  };
  handleRouteError.$inject = ['$rootScope', '$location'];
  mod.run(handleRouteError);
  return mod;
});
