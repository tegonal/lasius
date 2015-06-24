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
  
  def edit(bookingId: BookingId, start: DateTime, end: DateTime) = HasRole(FreeUser, parse.empty) {
	  implicit subject =>
	  implicit request => {
		  timeBookingManagerService ! EditBooking(subject.userId, bookingId, start, end)
		  Future.successful(Ok)
	  }
  }

  def pause(bookingId: BookingId, time: DateTime = DateTime.now()) = HasRole(FreeUser, parse.empty) {
    implicit subject =>
      implicit request => {
        timeBookingManagerService ! PauseBooking(subject.userId, bookingId, time)
        Future.successful(Ok)
      }
  }

  def resume(bookingId: BookingId, time: DateTime = DateTime.now()) = HasRole(FreeUser, parse.empty) {
    implicit subject =>
      implicit request => {
        timeBookingManagerService ! ResumeBooking(subject.userId, bookingId, time)
        Future.successful(Ok)
      }
  }

  def changeStart(bookingId: BookingId, newStart: DateTime) = HasRole(FreeUser, parse.empty) {
    implicit subject =>
      implicit request => {
        timeBookingManagerService ! ChangeStartTimeOfBooking(subject.userId, bookingId, newStart)
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