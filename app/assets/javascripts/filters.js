/** Common filters. */
define(['angular'], function(angular) {
  'use strict';

  var mod = angular.module('filters', []);
  /**
   * Extracts a given property from the value it is applied to.
   * {{{
   * (user | property:'name')
   * }}}
   */
  mod.filter('property', function(value, property) {
    if (angular.isObject(value)) {
      if (value.hasOwnProperty(property)) {
        return value[property];
      }
    }
  });
  
  mod.filter('percentage', ['$filter', function ($filter) {
    return function (input, decimals) {
      return $filter('number')(input * 100, decimals) + '%';
    };
  }]);
  
  mod.filter('sumFilter', function() {
    return function(items, property) {
        var total = 0;
        if (items === undefined) {
          return undefined;
        }
        for (var i=0; i<items.length; i++) {
          var value = items[i];
          if (angular.isObject(value)) {
            if (value.hasOwnProperty(property)) {
              total = total + value[property];
            }
            else if (angular.isFunction(property)) {
              var val = property(value);              
              total = total + val;
            }
          }
         }
        return total;
    };
  });
  
  /**
   * AngularJS default filter with the following expression:
   * "person in people | filter: {name: $select.search, age: $select.search}"
   * performs a AND between 'name: $select.search' and 'age: $select.search'.
   * We want to perform a OR.
   */
  mod.filter('propsFilter', function() {
    return function(items, props) {
      var out = [];

      if (angular.isArray(items)) {
        items.forEach(function(item) {
          var itemMatches = false;

          var keys = Object.keys(props);
          for (var i = 0; i < keys.length; i++) {
            var prop = keys[i];
            var text = props[prop].toLowerCase();
            if (item[prop].toString().toLowerCase().indexOf(text) !== -1) {
              itemMatches = true;
              break;
            }
          }

          if (itemMatches) {
            out.push(item);
          }
        });
      } else {
        // Let the output be the input untouched
        out = items;
      }

      return out;
    };
  });
  
  return mod;
});
