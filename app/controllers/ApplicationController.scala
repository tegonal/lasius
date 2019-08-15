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

import java.util.UUID

import actors.ClientMessagingWebsocketActor
import core.Global._
import models._
import org.mindrot.jbcrypt.BCrypt
import play.api._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json._
import play.api.libs.streams._
import play.api.mvc._
import play.api.mvc.WebSocket.MessageFlowTransformer
import repositories.{MongoSecurityRepositoryComponent, SecurityRepositoryComponent}
import Events._

import scala.concurrent.Future

class ApplicationController {
  self: Controller with SecurityRepositoryComponent with Security with CacheAware =>
    
    implicit val messageFlowTransformer = MessageFlowTransformer.jsonMessageFlowTransformer[InEvent, OutEvent]
    
    lazy val appConfig = loadApplicationConfig()
    lazy val playConfig = Play.current.configuration

    private def loadApplicationConfig() = {
      val ssl = playConfig.getBoolean("lasius.use_ssl").getOrElse(false)
      val title = playConfig.getString("lasius.title").getOrElse("Lasius")
      val instance = playConfig.getString("lasius.instance").getOrElse("Dev")
      ApplicationConfig(title, instance, ssl)
    }

  def index = Action {
    Ok(views.html.index())
  }

  /**
   * Provide access to actor based messaging websocket
   */
  def messagingSocket = WebSocket.acceptOrResult[InEvent, OutEvent] { implicit request =>
    Future.successful(checkToken() match {
      case Right(result) =>
        Logger.warn(s"Coudln't Websocket for client ${result.header} - ${result.body}")
        Left(result)
      case Left(subject) => Right({ 
        Logger.debug(s"Create Websocket for client ${subject.userId}")
        ActorFlow.actorRef(ClientMessagingWebsocketActor.props(subject.userId))
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
        cache.set(uuid, user.id)

        loginStateAggregate ! UserLoggedIn(user.id)

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
    cache.remove(subject.token)

    loginStateAggregate ! UserLoggedOut(subject.userId)

    //notify client
    ClientMessagingWebsocketActor ! (subject.userId, UserLoggedOut(subject.userId), List(subject.userId))

    Future.successful(Ok.discardingCookies(DiscardingCookie(name = AuthTokenCookieKey)))
  }
  
  /**
   * Load application config
   */
  def config = Action.async {
    Future.successful(Ok(Json.toJson(appConfig)))
  }
}

object ApplicationController extends ApplicationController
  with Controller
  with MongoSecurityRepositoryComponent
  with Security
  with DefaultSecurityComponent
  with DefaultCacheProvider