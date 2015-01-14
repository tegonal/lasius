package controllers

import akka.actor.ActorSystem

import services.TimeBookingManagerService

object Boot extends App {
  implicit val system = ActorSystem("seed-actor-system")
  implicit val executionContext = system.dispatcher
  implicit val timeBookingManagerService = Boot.system.actorOf(TimeBookingManagerService.props)
}