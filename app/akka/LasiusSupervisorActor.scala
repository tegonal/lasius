package akka

import akka.actor.Actor
import akka.actor.Props
     
class LasiusSupervisorActor extends Actor {
  import akka.actor.OneForOneStrategy
  import akka.actor.SupervisorStrategy._
  import scala.concurrent.duration._
 
  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
      case _                => Restart
    }
  
  def receive = {
    case p: Props => sender() ! context.actorOf(p)
   }
}

object LasiusSupervisorActor {
  def props: Props = Props(classOf[LasiusSupervisorActor])
}

