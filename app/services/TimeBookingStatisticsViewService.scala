/*   __                          __                                          *\
*   / /____ ___ ____  ___  ___ _/ /       lasius                      *
*  / __/ -_) _ `/ _ \/ _ \/ _ `/ /        contributed by tegonal              *
*  \__/\__/\_, /\___/_//_/\_,_/_/         http://tegonal.com/                 *
*         /___/                                                               *
*                                                                             *
* This program is free software: you can redistribute it and/or modify it     *
* under the terms of the GNU General Public License as published by    *
* the Free Software Foundation, either version 3 of the License,              *
* or (at your option) any later version.                                      *
*                                                                             *
* This program is distributed in the hope that it will be useful, but         *
* WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY  *
* or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for *
* more details.                                                               *
*                                                                             *
* You should have received a copy of the GNU General Public License along     *
* with this program. If not, see http://www.gnu.org/licenses/                 *
*                                                                             *
\*                                                                           */
package services

import akka.actor.Props
import domain.views.UserTimeBookingStatisticsView
import domain.views.UserTimeBookingStatisticsView.Ack
import models.UserId
import services.UserService.StartUserTimeBookingView

object TimeBookingStatisticsViewService {

  def props: Props = Props(classOf[TimeBookingStatisticsViewService])
}

class TimeBookingStatisticsViewService extends UserService[StartUserTimeBookingView] {

  def processCommand: Receive = {
    case Ack =>
    case cmd: StartUserTimeBookingView =>
      log.debug(s"TimeBookingHistoryViewService -> processCommand:$cmd")
      processAggregateCommand(cmd.userId, cmd)
    case c => log.debug(s"TimeBookingHistoryViewService -> unknown command:$c")
  }

  def aggregateProps(id: UserId) = UserTimeBookingStatisticsView.props(id)
}