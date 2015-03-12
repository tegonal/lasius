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
import repositories.SecurityRepositoryComponent
import repositories.UserRepository
import play.api.libs.concurrent.Execution.Implicits._
import org.mindrot.jbcrypt.BCrypt
import play.api.cache.Cache
import java.util.UUID
import play.api.libs.json._
import repositories.MongoSecurityRepositoryComponent
import domain.LoginStateAggregate

class ApplicationController {
  self: Controller with SecurityRepositoryComponent with Security =>

  def index = Action {
    Ok(views.html.index())
  }

  /**
   * Provide access to actor based messaging websocket
   */
  def messagingSocket = WebSocket.tryAcceptWithActor[InEvent, OutEvent] { implicit request =>
    Future.successful(checkToken() match {
      case Right(result) =>
        Logger.warn(s"Coudln't Websocket for client ${result.header} - ${result.body}")
        Left(result)
      case Left(subject) => Right({ out =>
        Logger.debug(s"Create Websocket for client ${subject.userId}")
        ClientMessagingWebsocketActor.props(out, subject.userId)
      })
    })
  }

  /**
   * Log-in a user.
   *
   * Set the cookie [[AuthTokenCookieKey]] to have AngularJS set the X-XSRF-TOKEN in the HTTP
   * header.
   *
   * returns The token needed for subsequent requests
   */
  def login(email: String, password: String) = Action.async { implicit request =>
    //first resolve using authentication service
    authenticate(email, password).map {
      case Right(error) =>
        error
      case Left(user) => {
        Logger.debug(s"Store token for user: $user")
        val uuid = UUID.randomUUID.toString
        Cache.set(uuid, user.id)
        
        loginStateAggregate ! LoginStateAggregate.UserLoggedIn(user.id)
        
        Ok(Json.obj("token" -> uuid))
          .withCookies(Cookie(AuthTokenCookieKey, uuid, None, httpOnly = false))
      }
    }
  }

  def authenticate(email: String, password: String): Future[Either[User, Result]] = {
    userRepository.findByEmail(email) map {
      case Some(user) =>
        //check password validation
        if (BCrypt.checkpw(password.toString, user.password)) {
          Left(user)
        } else {
          Right(BadRequest("Authentication failed"))
        }
      case None => Right(BadRequest("Authentication failed"))
    }
  }

  /**
   * Log-out a user. Invalidates the authentication token.
   *
   * Discard the cookie [[AuthTokenCookieKey]] to have AngularJS no longer set the
   * X-XSRF-TOKEN in HTTP header.
   */
  def logout() = HasToken(parse.empty) { subject =>
    implicit request =>
      doLogout(subject);
  }

  def doLogout(subject: Subject) = {
    Logger.debug(s"Remove token from cache: ${subject.token}")
    Cache.remove(subject.token)

    loginStateAggregate ! LoginStateAggregate.UserLoggedOut(subject.userId)
    
    //notify client
    ClientMessagingWebsocketActor ! (subject.userId, UserLoggedOut(subject.userId), List(subject.userId))

    Future.successful(Ok.discardingCookies(DiscardingCookie(name = AuthTokenCookieKey)))
  }
}

object ApplicationController extends ApplicationController
  with Controller
  with MongoSecurityRepositoryComponent
  with Security
  with DefaultSecurityComponent