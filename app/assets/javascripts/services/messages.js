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
define(['angular'], function (angular) {
  'use strict';
  
  
  var mod = angular.module('services.messages', ['ngCookies']);
  mod.factory('clientMessageService', ['$http', '$location', '$q', 'playRoutes', '$rootScope', 'msgBus', 'userService', 'Auth', function ($http, $location, $q, playRoutes, $rootScope, msgBus, userService, Auth) {
    
    var send = function(eventType, eventData) {
      //append type to event data
      var obj = JSON.stringify(eventData);
      var event = '{"type":"'+eventType+'",'+obj.substring(1);
      try {
        console.log("sending message: " + event);
        $rootScope.messagingSocket.send(event);
      } catch(err) {
        console.log("error sending message: " + err);
      }
     };
    
   return {
       //send message to server
       send: send,
        //start websocket based messaging
        start: function() {
          $rootScope.$watch(
              userService.getUser, 
              function() {
                Auth.isLoggedIn().then(function(loggedIn) {
                  if (loggedIn) {          
                    console.log('registering websocket');          
                    var wsUrl = playRoutes.controllers.ApplicationController.messagingSocket().webSocketUrl();
                    //append token to websocket url because normal http headers can't get controlled
                    var securedUrl = wsUrl+ "?auth="+userService.getToken();
                    if(!(angular.isDefined($rootScope.messagingSocket))) {
                      $rootScope.messagingSocket = new WebSocket(securedUrl);
                      $rootScope.messagingSocket.onmessage = function(msg) { 
                         var data = JSON.parse(msg.data);
                          msgBus.emitMsg(data);
                      };
                      $rootScope.messagingSocket.onopen = function (event) {
                        console.log('onopen : '+event);
                        //send hello command to server
                        send('HelloServer', {client: "someClientName"});
                      };
                    }                         
                  }
                });
            });
        }
     };
   }]);
 
  return mod;
});