package actors

import akka.actor._
import models._
import play.api.Logger
import play.api.libs.iteratee.Concurrent
import actors.ControlCommands._

object ControlCommands {
  case class SendToClient(senderUserId: UserId, event: OutEvent, receivers: List[UserId] = Nil)
}

trait ClientReceiverComponent {
  val clientReceiver: ClientReceiver
}

trait ClientReceiver {
  def broadcast(senderUserId: UserId, event: OutEvent)

  /**
   * Send OutEvent to a list of receiving clients exclusing sender itself
   */
  def send(senderUserId: UserId, event: OutEvent, receivers: List[UserId])

  def !(senderUserId: UserId, event: OutEvent, receivers: List[UserId])
}

trait DefaultClientReceiverComponent extends ClientReceiverComponent {
  val clientReceiver: ClientReceiver = ClientMessagingWebsocketActor
}

object ClientMessagingWebsocketActor extends ClientReceiver {
  def props(out: ActorRef, userId: UserId) = Props(new ClientMessagingWebsocketActor(out, userId))
  var actors: Map[String, ActorRef] = Map()

  /**
   * Broadcast OutEvent to every client except sender itself
   */
  def broadcast(senderUserId: UserId, event: OutEvent) = {
    actors.values.map(_ ! SendToClient(senderUserId, event))
  }

  /**
   * Send OutEvent to a list of receiving clients exclusing sender itself
   */
  def send(senderUserId: UserId, event: OutEvent, receivers: List[UserId]) = {
    actors.values.map(_ ! SendToClient(senderUserId, event, receivers))
  }

  def !(senderUserId: UserId, event: OutEvent, receivers: List[UserId]) = {
    send(senderUserId, event, receivers)
  }
}

class ClientMessagingWebsocketActor(out: ActorRef, userId: UserId) extends Actor {
  //val (enumerator, channel) = Concurrent.broadcast[OutEvent]

  //append to map of active actors
  ClientMessagingWebsocketActor.actors += (userId.value -> self)

  def receive = {
    case HelloServer(client) =>
      Logger.debug(s"Received HelloServer($client)")

      out ! HelloClient
    case SendToClient(senderUserId, event, Nil) =>
      //broadcast to all others
      if (senderUserId != userId) {
        out ! event
      }
    case SendToClient(senderUserId, event, receivers) =>
      //send to specific clients only
      if (receivers.contains(userId)) {
        out ! event
      }
  }

  def broadcast(event: OutEvent) = {
    ClientMessagingWebsocketActor.actors.values.map(_ ! event)
  }

  override def postStop() = {
    //remove from active actors
    ClientMessagingWebsocketActor.actors -= (userId.value)
    super.postStop
  }
}