package core

import akka.actor.{ActorRef, ActorSystem}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import models.UserId

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

trait SystemServices extends ActorSystemAware {
  val systemUser: UserId
  val supervisor: ActorRef
  implicit val timeout: Timeout
  val duration: FiniteDuration
  val timeBookingViewService: ActorRef

  val loginStateAggregate: ActorRef
  val loginHandler: ActorRef

  val currentUserTimeBookingsViewService: ActorRef
  val currentTeamTimeBookingsView: ActorRef
  val latestUserTimeBookingsViewService: ActorRef
  val timeBookingStatisticsViewService: ActorRef
  val tagCache: ActorRef
  val pluginHandler: ActorRef
}

trait SystemServicesAware {
  val systemServices: SystemServices
  implicit val system: ActorSystem
  implicit val timeout: Timeout
  implicit val materializer: ActorMaterializer
  implicit val ec: ExecutionContext
}

trait DefaultSystemServicesAware extends SystemServicesAware {
  val systemServices: SystemServices = DefaultServices.getInstance()
  implicit val system: ActorSystem = DefaultServices.getInstance().system
  implicit val materializer = DefaultServices.getInstance().materializer
  implicit val ec = DefaultServices.getInstance().ec
  implicit val timeout: Timeout = DefaultServices.getInstance().timeout
}
