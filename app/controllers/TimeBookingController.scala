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

import core.{DefaultSystemServicesAware, SystemServicesAware}
import domain.UserTimeBookingAggregate._
import models._
import org.joda.time.DateTime
import play.api.mvc.Controller
import play.api.Logger

import scala.concurrent.Future

class TimeBookingController {
  self: Controller with Security with SystemServicesAware =>

  def start(categoryId: CategoryId, projectId: ProjectId, tags: Seq[TagId], start: DateTime = DateTime.now()) = HasRole(FreeUser, parse.empty) {
    implicit subject =>
      implicit request => {
        Logger.debug(s"TimeBokingController -> start - userId:${subject.userId}, projectId: $projectId, tags:$tags, start:$start")
        systemServices.timeBookingViewService ! StartBooking(subject.userId, categoryId, projectId, tags, start)
        Future.successful(Ok)
      }
  }

  def stop(bookingId: BookingId, end: DateTime = DateTime.now()) = HasRole(FreeUser, parse.empty) {
    implicit subject =>
      implicit request => {
        systemServices.timeBookingViewService ! EndBooking(subject.userId, bookingId, end)
        Future.successful(Ok)
      }
  }

  def remove(bookingId: BookingId) = HasRole(FreeUser, parse.empty) {
    implicit subject =>
      implicit request => {
        systemServices.timeBookingViewService ! RemoveBooking(subject.userId, bookingId)
        Future.successful(Ok)
      }
  }

  def edit(bookingId: BookingId, start: DateTime, end: DateTime) = HasRole(FreeUser, parse.empty) {
    implicit subject =>
      implicit request => {
        systemServices.timeBookingViewService ! EditBooking(subject.userId, bookingId, start, end)
        Future.successful(Ok)
      }
  }

  def pause(bookingId: BookingId, time: DateTime = DateTime.now()) = HasRole(FreeUser, parse.empty) {
    implicit subject =>
      implicit request => {
        systemServices.timeBookingViewService ! PauseBooking(subject.userId, bookingId, time)
        Future.successful(Ok)
      }
  }

  def resume(bookingId: BookingId, time: DateTime = DateTime.now()) = HasRole(FreeUser, parse.empty) {
    implicit subject =>
      implicit request => {
        systemServices.timeBookingViewService ! ResumeBooking(subject.userId, bookingId, time)
        Future.successful(Ok)
      }
  }

  def changeStart(bookingId: BookingId, newStart: DateTime) = HasRole(FreeUser, parse.empty) {
    implicit subject =>
      implicit request => {
        systemServices.timeBookingViewService ! ChangeStartTimeOfBooking(subject.userId, bookingId, newStart)
        Future.successful(Ok)
      }
  }

  def add(categoryId: CategoryId, projectId: ProjectId, tags: Seq[TagId], start: DateTime, end: DateTime, comment: Option[String]) = HasRole(FreeUser, parse.empty) {
    implicit subject =>
      implicit request => {
        systemServices.timeBookingViewService ! AddBooking(subject.userId, categoryId, projectId, tags, start, end, comment)
        Future.successful(Ok)
      }
  }
}

object TimeBookingController extends TimeBookingController  with Controller with Security with DefaultSecurityComponent with DefaultCacheProvider with DefaultSystemServicesAware