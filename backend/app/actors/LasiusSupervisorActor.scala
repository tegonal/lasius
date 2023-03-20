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

import akka.actor.{Actor, Props}

import scala.language.postfixOps

class LasiusSupervisorActor extends Actor {
  import akka.actor.OneForOneStrategy
  import akka.actor.SupervisorStrategy._

  import scala.concurrent.duration._

  override val supervisorStrategy: OneForOneStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
      case _ =>
        Restart
    }

  def receive: Receive = { case p: Props =>
    sender() ! context.actorOf(p)
  }
}

object LasiusSupervisorActor {
  def props: Props = Props(classOf[LasiusSupervisorActor])

  final val name = "LasiusSupervisorActor"
}
