package controllers

import play.api.mvc.Controller
import services.TimeBookingManagerService
import models._
import org.joda.time.DateTime
import play.api.mvc.Action
import domain.UserTimeBookingAggregate._
import akka.actor.ActorRef
import Boot._

class TimeBookingController {
  self: Controller =>

  def start(userId: UserId, projectId: ProjectId, tags: Seq[TagId], start: DateTime = DateTime.now()) = Action {
    timeBookingManagerService ! StartBooking(userId, projectId, tags, start)
    Ok
  }

  def stop(userId: UserId, bookingId: BookingId, end: DateTime = DateTime.now()) = Action {
    timeBookingManagerService ! EndBooking(userId, bookingId, end)
    Ok
  }

  def remove(userId: UserId, bookingId: BookingId) = Action {
    timeBookingManagerService ! RemoveBooking(userId, bookingId)
    Ok
  }

  def append(userId: UserId, projectId: ProjectId, tags: Seq[TagId], start: DateTime, end: DateTime) = Action {
    timeBookingManagerService ! AppendBooking(userId, projectId, tags, start, end)
    Ok
  }
}

object TimeBookingController extends TimeBookingController with Controller {

}