/**
 * Configure routes of common module.
 */
define(['angular', 
        './controllers/dashboardController'], 
    function(angular, dashboardController) {
  'use strict';

  var mod = angular.module('routes', []);

  mod.config(['$routeProvider', function($routeProvider) {
    $routeProvider
      .when('/', {templateUrl: '/assets/dashboard.html', controller:dashboardController.DashboardCtrl})
      .otherwise({redirectTo: '/assets/notFound.html'});
  }]);
  
  mod.controller('DashboardCtrl', dashboardController.DashboardCtrl);

  return mod;
});
