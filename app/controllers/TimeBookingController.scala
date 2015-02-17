package controllers

import play.api.mvc.Controller

import services.TimeBookingViewService
import models._
import org.joda.time.DateTime
import play.api.mvc.Action
import domain.UserTimeBookingAggregate._
import akka.actor.ActorRef
import core.Global._
import play.api.Logger

class TimeBookingController {
  self: Controller =>

  def start(userId: UserId, categoryId: CategoryId, projectId: ProjectId, tags: Seq[TagId], start: DateTime = DateTime.now()) = Action {
    Logger.debug(s"TimeBokingController -> start - userId:$userId, projectId: $projectId, tags:$tags, start:$start")
    timeBookingManagerService ! StartBooking(userId, categoryId, projectId, tags, start)
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

  def append(userId: UserId, categoryId: CategoryId, projectId: ProjectId, tags: Seq[TagId], start: DateTime, end: DateTime) = Action {
    timeBookingManagerService ! AppendBooking(userId, categoryId, projectId, tags, start, end)
    Ok
  }
}

object TimeBookingController extends TimeBookingController with Controller {

}