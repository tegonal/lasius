import akka.actor.ActorSystem

object Boot extends App {
  implicit val system = ActorSystem("seed-actor-system")

  implicit val executionContext = system.dispatcher

}