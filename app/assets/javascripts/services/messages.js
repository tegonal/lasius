/**
 * ClientMessage service
 */
define(['angular'], function (angular) {
  'use strict';
  
  
  var mod = angular.module('services.messages', ['ngCookies']);
  mod.factory('clientMessageService', ['$http', '$location', '$q', 'playRoutes', '$rootScope', 'msgBus', function ($http, $location, $q, playRoutes, $rootScope, msgBus) {
    
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
          //$rootScope.$watch(
             // userService.getUser, 
              //function() {
                //Auth.isLoggedIn().then(function(loggedIn) {
                  //if (loggedIn) {          
                    console.log('registering websocket');          
                    var wsUrl = playRoutes.controllers.Application.messagingSocket().webSocketUrl();
                    //append token to websocket url because normal http headers can't get controlled
                    var securedUrl = wsUrl;//+ "?auth="+userService.getToken();
                    if(!(angular.isDefined($rootScope.messagingSocket))) {
                      $rootScope.messagingSocket = new WebSocket(securedUrl);
                      $rootScope.messagingSocket.onmessage = function(msg) { 
                         var data = JSON.parse(msg.data);
                          console.log('received : ');
                          console.log(data);
                          msgBus.emitMsg(data);
                      };
                      $rootScope.messagingSocket.onopen = function (event) {
                        console.log('onopen : '+event);
                        //send hello command to server
                        send('HelloServer', {client: "someClientName"});
                      };
                    }                         
                 // }
                //});
            //});
        }
     };
   }]);
 
  return mod;
});