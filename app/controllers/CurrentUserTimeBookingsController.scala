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
  def index(userId: UserId) = Action.async {
    implicit val timeout = Timeout(5 second)
    (currentUserTimeBookingsViewService ? GetCurrentTimeBooking(userId)).mapTo[CurrentUserTimeBooking].map { resp: CurrentUserTimeBooking =>
      Ok(views.html.currentUserTimeBookingsView(resp.userId, resp.booking))
    }
  }

  def getCurrentTimeBooking(userId: UserId) = Action.async {
    implicit val timeout = Timeout(5 second)
    (currentUserTimeBookingsViewService ? GetCurrentTimeBooking(userId)).mapTo[CurrentUserTimeBooking].map { resp: CurrentUserTimeBooking =>
      Ok(Json.obj("booking" -> resp.booking))
    }
  }
}

object CurrentUserTimeBookingsController extends CurrentUserTimeBookingsController with Controller {

}