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

package services

import actors.ClientReceiver
import akka.actor.Props
import akka.pattern.StatusReply.Ack
import domain.views.LatestUserTimeBookingsView
import domain.views.LatestUserTimeBookingsView._
import domain.AggregateRoot.{
  ForwardPersistentEvent,
  InitializeViewLive,
  RestoreViewFromState
}
import models.UserId.UserReference
import models.{EntityReference, UserId}

object LatestUserTimeBookingsViewService {

  def props(clientReceiver: ClientReceiver): Props =
    Props(classOf[LatestUserTimeBookingsViewService], clientReceiver)
}

class LatestUserTimeBookingsViewService(clientReceiver: ClientReceiver)
    extends UserService[
      domain.views.LatestUserTimeBookingsView.GetLatestTimeBooking] {

  override def processCommand: Receive = {
    case Ack =>
    case cmd: GetLatestTimeBooking =>
      processAggregateCommand(cmd.userReference, cmd)
    case r: RestoreViewFromState =>
      restoreViewFromState(r)
    case f: ForwardPersistentEvent =>
      forwardPersistentEvent(f)
    case i: InitializeViewLive =>
      initializeViewLive(i)
    case c =>
      log.debug(s"CurrentUserTimeBookingsViewService -> unknown command:$c")
  }

  override def aggregateProps(id: UserReference): Props =
    LatestUserTimeBookingsView.props(clientReceiver, id)
}
