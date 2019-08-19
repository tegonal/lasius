package core

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

import scala.concurrent.ExecutionContext

trait ActorSystemAware {
  implicit val system: ActorSystem
  implicit lazy val materializer = ActorMaterializer()
  implicit lazy val ec: ExecutionContext = system.dispatcher
}