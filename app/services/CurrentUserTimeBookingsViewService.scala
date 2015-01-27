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
import views.CurrentUserTimeBookingsView._
import views.CurrentUserTimeBookingsView

object CurrentUserTimeBookingsViewService {

  def props: Props = Props(new CurrentUserTimeBookingsViewService)
}

class CurrentUserTimeBookingsViewService extends UserService[views.CurrentUserTimeBookingsView.GetCurrentTimeBooking] {

  import domain.UserTimeBookingAggregate._

  def processCommand: Receive = {
    case cmd: GetCurrentTimeBooking =>
      log.debug(s"CurrentUserTimeBookingsViewService -> processCommand:$cmd -> $sender")
      processAggregateCommand(cmd.userId, cmd)
    case c => log.debug(s"CurrentUserTimeBookingsViewService -> unknown command:$c")
  }

  def aggregateProps(id: UserId) = CurrentUserTimeBookingsView.props(id)
}