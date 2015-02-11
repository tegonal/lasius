package controllers

import play.api._
import play.api.mvc._
import models._
import models.Events._
import scala.concurrent.Future
import actors.ClientMessagingWebsocketActor
import play.api.Play.current
import services.UserService.StartUserTimeBookingView
import core.Global._

object Application extends Controller {

  def index = Action {
    Ok(views.html.index())
  }

  /**
   * Provide access to actor based messaging websocket
   */
  def messagingSocket = WebSocket.acceptWithActor[InEvent, OutEvent] { request =>
    out =>
      ClientMessagingWebsocketActor.props(out, UserId("noob"))
  }
}