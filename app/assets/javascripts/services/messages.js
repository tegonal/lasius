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
  mod.factory('clientMessageService', ['$http', '$location', '$q', '$interval', 'playRoutes', '$rootScope', 'msgBus', 'userService', 'Auth', 'appConfigService', function ($http, $location, $q, $interval, playRoutes, $rootScope, msgBus, userService, Auth, appConfigService) {
    
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
     
     var reconnectPromise;
     var connected = false;
     
     var tryOpen = function(url) {
       //retry opening WebSocket every 5 seconds
         if(angular.isUndefined(reconnectPromise)) {
           reconnectPromise = $interval(function() {
             $rootScope.messagingSocket = openWebSocket(url);
           }, 5000);
         }
       };

     var openWebSocket = function(url) {
       var ws = new WebSocket(url);           

       ws.onmessage = function(msg) {
         var data = JSON.parse(msg.data);
         console.log('WS received event', data);
         msgBus.emitMsg(data);
       };
       ws.onopen = function(event) {
         console.log('WS onopen : ' + event);
         msgBus.emitMsg({type: 'WebSocketOpen' });
         connected = true;
         //send hello command to server
         send('HelloServer', {client: "someClientName"});

         if(!angular.isUndefined(reconnectPromise)) {
           $interval.cancel(reconnectPromise);
           reconnectPromise = undefined;           
         }         
       };
       ws.onclose = function (event) {
         var reason;
         // See http://tools.ietf.org/html/rfc6455#section-7.4.1
         if (event.code === 1000) {
           reason = 'Normal closure, meaning that the purpose for which the connection was established has been fulfilled.';
         } else if(event.code === 1001) {
           reason = 'An endpoint is \'going away\', such as a server going down or a browser having navigated away from a page.';
         } else if(event.code === 1002) {
           reason = 'An endpoint is terminating the connection due to a protocol error';
         } else if(event.code === 1003) {
           reason = 'An endpoint is terminating the connection because it has received a type of data it cannot accept (e.g., an endpoint that understands only text data MAY send this if it receives a binary message).';
         } else if(event.code === 1004) {
           reason = 'Reserved. The specific meaning might be defined in the future.';
         } else if(event.code === 1005) {
           reason = 'No status code was actually present.';
         } else if(event.code === 1006) {
           reason = 'The connection was closed abnormally, e.g., without sending or receiving a Close control frame';
         } else if(event.code === 1007) {
           reason = 'An endpoint is terminating the connection because it has received data within a message that was not consistent with the type of the message (e.g., non-UTF-8 [http://tools.ietf.org/html/rfc3629] data within a text message).';
         } else if(event.code === 1008) {
           reason = 'An endpoint is terminating the connection because it has received a message that \'violates its policy\'. This reason is given either if there is no other sutible reason, or if there is a need to hide specific details about the policy.';
         } else if(event.code === 1009) {
           reason = 'An endpoint is terminating the connection because it has received a message that is too big for it to process.';
         } else if(event.code === 1010) { // Note that this status code is not used by the server, because it can fail the WebSocket handshake instead.
           reason = 'An endpoint (client) is terminating the connection because it has expected the server to negotiate one or more extension, but the server didn\'t return them in the response message of the WebSocket handshake. <br /> Specifically, the extensions that are needed are: ' + event.reason;
         } else if(event.code === 1011) {
           reason = 'A server is terminating the connection because it encountered an unexpected condition that prevented it from fulfilling the request.';
         } else if(event.code === 1015) {
           reason = 'The connection was closed due to a failure to perform a TLS handshake (e.g., the server certificate can\'t be verified).';
         } else {
           reason = 'Unknown reason';
         }
         console.log('WS closed with a reason:' + reason);
         msgBus.emitMsg({type: 'WebSocketClosed', reason: reason });
         connected = false;

         tryOpen(url);
       };
       tryOpen(url);
       return ws;
     };
    
   return {
     
       //send message to server
       send: send,
       connected: function(){return connected;},
        //start websocket based messaging
        start: function() {
          $rootScope.$watch(
              userService.getUser,
              function() {
                Auth.isLoggedIn().then(function(loggedIn) {
                  if (loggedIn) { 
                    appConfigService.resolveConfig().then(function(config) {
                      console.log('registering websocket', config.ssl);          
                      var wsUrl = playRoutes.controllers.ApplicationController.messagingSocket().webSocketUrl(config.ssl); // add param true to force wss://
                      //append token to websocket url because normal http headers can't get controlled
                      var securedUrl = wsUrl+ "?auth="+userService.getToken();
                      if (!(angular.isDefined($rootScope.messagingSocket))) {
                        $rootScope.messagingSocket = openWebSocket(securedUrl);
                      }
                    });                    
                  }
                });
            });
        }
     };
   }]);
 
  return mod;
});