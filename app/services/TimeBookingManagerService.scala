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

object TimeBookingViewService {

  def props: Props = Props(new TimeBookingViewService)
}

class TimeBookingViewService extends UserService[domain.UserTimeBookingAggregate.UserTimeBookingCommand] {

  import domain.UserTimeBookingAggregate._

  def processCommand: Receive = {
    case cmd: UserTimeBookingCommand =>
      log.debug(s"TimeBookingManagerService -> processCommand:$cmd")
      processAggregateCommand(cmd.userId, cmd)
    case c => log.debug(s"TimeBookingManagerService -> unknown command:$c")
  }

  def aggregateProps(id: UserId) = UserTimeBookingAggregate.props(id)
}