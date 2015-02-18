package services

import akka.actor.ActorLogging
import akka.actor.Actor
import akka.actor.ActorRef
import domain.UserTimeBookingAggregate
import akka.actor.Terminated
import models.UserId
import models.ProjectId
import models.TagId
import org.joda.time.DateTime
import akka.actor.Props
import domain.views.UserTimeBookingHistoryView
import services.UserService.StartUserTimeBookingView
import domain.views.UserTimeBookingStatisticsView

object TimeBookingStatisticsViewService {

  def props: Props = Props(new TimeBookingStatisticsViewService)
}

class TimeBookingStatisticsViewService extends UserService[StartUserTimeBookingView] {

  import domain.UserTimeBookingAggregate._

  def processCommand: Receive = {
    case cmd: StartUserTimeBookingView =>
      log.debug(s"TimeBookingHistoryViewService -> processCommand:$cmd")
      processAggregateCommand(cmd.userId, cmd)
    case c => log.debug(s"TimeBookingHistoryViewService -> unknown command:$c")
  }

  def aggregateProps(id: UserId) = UserTimeBookingStatisticsView.props(id)
}