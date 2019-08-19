package core

import akka.actor.{ActorRef, ActorSystem}

trait LoginHandlerAware extends ActorSystemAware {
  val loginHandler: ActorRef
}

trait DefaultLoginHandlerAware extends LoginHandlerAware {
  implicit val system: ActorSystem = DefaultLoginHandler.getInstance().system
  override lazy val loginHandler: ActorRef = DefaultLoginHandler.getInstance().loginHandler
}