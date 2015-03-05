/**
 * Configure routes of common module.
 */
define(['angular',
        './controllers/loginController',
        './controllers/dashboardController'], 
    function(angular, loginController, dashboardController) {
  'use strict';

  var mod = angular.module('routes', []);

  mod.config(['$routeProvider', function($routeProvider) {
    $routeProvider
      .when('/', {templateUrl: '/assets/home.html', controller:dashboardController.DashboardCtrl, access:userRoles.FreeUser})
      .when('/login', {templateUrl: '/assets/login.html', controller:loginController.LoginCtrl, access:userRoles.Guest})
      .when('/forbidden', {templateUrl:'/assets/forbidden.html', access:userRoles.Guest})
      .otherwise({redirectTo: '/assets/notFound.html'});
  }]);
  
  mod.controller('DashboardCtrl', dashboardController.DashboardCtrl);

  return mod;
});
