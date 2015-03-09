package controllers

import play.api.mvc.Controller

import models.UserId
import play.api.mvc.Action
import core.Global._
import domain.views.CurrentUserTimeBookingsView._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json._
import models.FreeUser
import scala.concurrent.Future

class CurrentUserTimeBookingsController {
  self: Controller with Security =>

  def getCurrentTimeBooking() = HasRole(FreeUser, parse.empty) {
    implicit subject =>
      implicit request => {
        currentUserTimeBookingsViewService ! GetCurrentTimeBooking(subject.userId)
        Future.successful(Ok)
      }
  }
}

object CurrentUserTimeBookingsController extends CurrentUserTimeBookingsController with Controller with Security with DefaultSecurityComponent