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
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Future

class TimeBookingController {
  self: Controller with Security =>

  def start(categoryId: CategoryId, projectId: ProjectId, tags: Seq[TagId], start: DateTime = DateTime.now()) = HasRole(FreeUser, parse.empty) {
    implicit subject =>
      implicit request => {
        Logger.debug(s"TimeBokingController -> start - userId:${subject.userId}, projectId: $projectId, tags:$tags, start:$start")
        timeBookingManagerService ! StartBooking(subject.userId, categoryId, projectId, tags, start)
        Future.successful(Ok)
      }
  }

  def stop(bookingId: BookingId, end: DateTime = DateTime.now()) = HasRole(FreeUser, parse.empty) {
    implicit subject =>
      implicit request => {
        timeBookingManagerService ! EndBooking(subject.userId, bookingId, end)
        Future.successful(Ok)
      }
  }

  def remove(bookingId: BookingId) = HasRole(FreeUser, parse.empty) {
    implicit subject =>
      implicit request => {
        timeBookingManagerService ! RemoveBooking(subject.userId, bookingId)
        Future.successful(Ok)
      }
  }

  def append(categoryId: CategoryId, projectId: ProjectId, tags: Seq[TagId], start: DateTime, end: DateTime) = HasRole(FreeUser, parse.empty) {
    implicit subject =>
      implicit request => {
        timeBookingManagerService ! AppendBooking(subject.userId, categoryId, projectId, tags, start, end)
        Future.successful(Ok)
      }
  }
}

object TimeBookingController extends TimeBookingController with Controller with Security with DefaultSecurityComponent