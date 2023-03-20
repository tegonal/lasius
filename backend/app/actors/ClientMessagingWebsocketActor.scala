/*
 *
 * Lasius - Open source time tracker for teams
 * Copyright (c) Tegonal Genossenschaft (https://tegonal.com)
 *
 * This file is part of Lasius.
 *
 * Lasius is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * Lasius is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Lasius. If not, see <https://www.gnu.org/licenses/>.
 */

package actors

import actors.ControlCommands._
import akka.actor._
import com.google.inject.ImplementedBy
import models._

object ControlCommands {
  case class SendToClient(senderUserId: UserId,
                          event: OutEvent,
                          receivers: List[UserId] = Nil)
}

@ImplementedBy(classOf[ClientReceiverWebsocket])
trait ClientReceiver {
  def broadcast(senderUserId: UserId, event: OutEvent): Unit

  /** Send OutEvent to a list of receiving clients exclusing sender itself
    */
  def send(senderUserId: UserId, event: OutEvent, receivers: List[UserId]): Unit

  def !(senderUserId: UserId, event: OutEvent, receivers: List[UserId]): Unit
}

class ClientReceiverWebsocket extends ClientReceiver {

  /** Broadcast OutEvent to every client except sender itself
    */
  def broadcast(senderUserId: UserId, event: OutEvent) = {
    ClientMessagingWebsocketActor.actors.values
      .map(_ ! SendToClient(senderUserId, event))
  }

  /** Send OutEvent to a list of receiving clients exclusing sender itself
    */
  def send(senderUserId: UserId, event: OutEvent, receivers: List[UserId]) = {
    ClientMessagingWebsocketActor.actors.values
      .map(_ ! SendToClient(senderUserId, event, receivers))
  }

  def !(senderUserId: UserId, event: OutEvent, receivers: List[UserId]) = {
    send(senderUserId, event, receivers)
  }
}

object ClientMessagingWebsocketActor {
  def props(userId: UserId)(out: ActorRef) =
    Props(new ClientMessagingWebsocketActor(out, userId))
  var actors: Map[UserId, ActorRef] = Map()
}

class ClientMessagingWebsocketActor(out: ActorRef, userId: UserId)
    extends Actor
    with ActorLogging {
  // val (enumerator, channel) = Concurrent.broadcast[OutEvent]

  // append to map of active actors
  ClientMessagingWebsocketActor.actors += (userId -> self)

  def receive = {
    case HelloServer(client) =>
      log.debug(s"Received HelloServer($client)")

      out ! HelloClient
    case Ping =>
      out ! Pong
    case SendToClient(senderUserId, event, Nil) =>
      // broadcast to all others
      if (senderUserId != userId) {
        out ! event
      }
    case SendToClient(senderUserId, event, receivers) =>
      // send to specific clients only
      if (receivers.contains(userId)) {
        out ! event
      }
  }

  def broadcast(event: OutEvent) = {
    ClientMessagingWebsocketActor.actors.values.map(_ ! event)
  }

  override def postStop() = {
    // remove from active actors
    ClientMessagingWebsocketActor.actors -= (userId)
    log.debug(s"Websocket connection closed for user ${userId.value}")
    super.postStop()
  }
}
