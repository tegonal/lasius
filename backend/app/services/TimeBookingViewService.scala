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
import domain.UserTimeBookingAggregate
import models.UserId.UserReference
import models.{EntityReference, UserId}
import play.modules.reactivemongo.ReactiveMongoApi
import repositories.BookingHistoryRepository

object TimeBookingViewService {

  def props(systemServices: SystemServices,
            clientReceiver: ClientReceiver,
            bookingHistoryRepository: BookingHistoryRepository,
            reactiveMongoApi: ReactiveMongoApi): Props =
    Props(classOf[TimeBookingViewService],
          systemServices,
          clientReceiver,
          bookingHistoryRepository,
          reactiveMongoApi)
}

class TimeBookingViewService(systemServices: SystemServices,
                             clientReceiver: ClientReceiver,
                             bookingHistoryRepository: BookingHistoryRepository,
                             reactiveMongoApi: ReactiveMongoApi)
    extends UserService[
      domain.UserTimeBookingAggregate.UserTimeBookingCommand] {

  import domain.UserTimeBookingAggregate._

  override def processCommand: Receive = {
    case cmd: UserTimeBookingCommand =>
      log.debug(s"TimeBookingManagerService -> processCommand:$cmd")
      processAggregateCommand(cmd.userReference, cmd)
    case c => log.debug(s"TimeBookingManagerService -> unknown command:$c")
  }

  override def aggregateProps(id: UserReference): Props =
    UserTimeBookingAggregate.props(systemServices,
                                   clientReceiver,
                                   bookingHistoryRepository,
                                   id,
                                   reactiveMongoApi)
}
