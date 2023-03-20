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
import core.SystemServices
import domain.AggregateRoot.{
  ForwardPersistentEvent,
  InitializeViewLive,
  RestoreViewFromState
}
import domain.views.UserTimeBookingStatisticsView
import models.{EntityReference, UserId}
import repositories.{BookingByProjectRepository, BookingByTagRepository}
import services.UserService.StartUserTimeBookingView
import akka.pattern.StatusReply.Ack
import models.UserId.UserReference
import play.modules.reactivemongo.ReactiveMongoApi

object TimeBookingStatisticsViewService {

  def props(clientReceiver: ClientReceiver,
            systemServices: SystemServices,
            bookingByProjectRepository: BookingByProjectRepository,
            bookingByTagRepository: BookingByTagRepository,
            reactiveMongoApi: ReactiveMongoApi): Props =
    Props(classOf[TimeBookingStatisticsViewService],
          clientReceiver,
          systemServices,
          bookingByProjectRepository,
          bookingByTagRepository,
          reactiveMongoApi)
}

class TimeBookingStatisticsViewService(
    clientReceiver: ClientReceiver,
    systemServices: SystemServices,
    bookingByProjectRepository: BookingByProjectRepository,
    bookingByTagRepository: BookingByTagRepository,
    reactiveMongoApi: ReactiveMongoApi)
    extends UserService[StartUserTimeBookingView] {

  override def processCommand: Receive = {
    case Ack =>
    case cmd: StartUserTimeBookingView =>
      log.debug(s"TimeBookingHistoryViewService -> processCommand:$cmd")
      processAggregateCommand(cmd.userReference, cmd)
    case r: RestoreViewFromState =>
      restoreViewFromState(r)
    case f: ForwardPersistentEvent =>
      forwardPersistentEvent(f)
    case i: InitializeViewLive =>
      initializeViewLive(i)
    case c => log.debug(s"TimeBookingHistoryViewService -> unknown command:$c")
  }

  override def aggregateProps(id: UserReference): Props =
    UserTimeBookingStatisticsView.props(clientReceiver,
                                        systemServices,
                                        bookingByProjectRepository,
                                        bookingByTagRepository,
                                        id,
                                        reactiveMongoApi)
}
