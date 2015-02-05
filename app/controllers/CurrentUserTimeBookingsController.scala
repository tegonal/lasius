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

class CurrentUserTimeBookingsController {
  self: Controller =>

  def getCurrentTimeBooking(userId: UserId) = Action {
    currentUserTimeBookingsViewService ! GetCurrentTimeBooking(userId)
    Ok
  }
}

object CurrentUserTimeBookingsController extends CurrentUserTimeBookingsController with Controller {
}