/*
 *
 * Lasius - Open source time tracker for teams
 * Copyright (c) Tegonal Genossenschaft (https://tegonal.com)
 *
 * This file is part of Lasius.
 *
 * Lasius is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * Lasius is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Lasius. If not, see <https://www.gnu.org/licenses/>.
 */

package controllers

import actors.{ClientMessagingWebsocketActor, ClientReceiver}
import akka.actor.ActorSystem
import akka.stream.{Materializer, OverflowStrategy}
import akka.util.Timeout
import com.typesafe.config.Config
import core.{CacheAware, DBSession, DBSupport, SystemServices}
import models._
import play.api._
import play.api.cache.AsyncCacheApi
import play.api.libs.json._
import play.api.libs.streams._
import play.api.mvc.WebSocket.MessageFlowTransformer
import play.api.mvc._
import play.modules.reactivemongo.ReactiveMongoApi
import repositories.UserRepository

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

case class LoginForm(email: String, password: String)

object LoginForm {
  implicit val loginFormFormat: OFormat[LoginForm] = Json.format[LoginForm]
}

class ApplicationController @Inject() (
    controllerComponents: ControllerComponents,
    userRepository: UserRepository,
    override val authConfig: AuthConfig,
    override val cache: AsyncCacheApi,
    override val systemServices: SystemServices,
    clientReceiver: ClientReceiver)(implicit
    executionContext: ExecutionContext,
    config: Config,
    override val reactiveMongoApi: ReactiveMongoApi)
    extends BaseLasiusController(controllerComponents) {

  implicit val timeout: Timeout = systemServices.timeout

  implicit val system: ActorSystem        = systemServices.system
  implicit val materializer: Materializer = Materializer.matFromSystem

  implicit val messageFlowTransformer
      : MessageFlowTransformer[InEvent, OutEvent] =
    MessageFlowTransformer.jsonMessageFlowTransformer[InEvent, OutEvent]

  val appConfig: ApplicationConfig = loadApplicationConfig()

  private def loadApplicationConfig() = {
    systemServices.initialize()

    val title    = config.getString("lasius.title")
    val instance = config.getString("lasius.instance")
    ApplicationConfig(title, instance)
  }

  /** Provide access to actor based messaging websocket
    */
  def messagingSocket: WebSocket = WebSocket.acceptOrResult[InEvent, OutEvent] {
    implicit request =>
      checkToken().map {
        case Right(result) =>
          logger.warn(
            s"Couldn't create Websocket for client ${result.header} - ${result.body}")
          Left(result)
        case Left(subject) =>
          Right({
            logger.debug(
              s"Create Websocket for client ${subject.userReference.id}")
            ActorFlow.actorRef(
              ClientMessagingWebsocketActor.props(subject.userReference.id),
              1000,
              OverflowStrategy.dropNew)
          })
      }
  }

  /** Log-in a user.
    *
    * Set the cookie [[AuthTokenCookieKey]] to have AngularJS set the
    * X-XSRF-TOKEN in the HTTP header.
    *
    * returns The token needed for subsequent requests
    */
  def login: Action[LoginForm] =
    Action.async(validateJson[LoginForm]) { implicit request =>
      // first resolve using authentication service
      withDBSession() { implicit dbSession =>
        userRepository
          .authenticate(request.body.email, request.body.password)
          .map {
            case None =>
              BadRequest("Authentication failed")
            case Some(user) =>
              logger.debug(s"Store token for user: ${user.reference}")
              val uuid = UUID.randomUUID.toString
              cache.set(uuid, user.reference)

              systemServices.loginStateAggregate ! UserLoggedInV2(
                user.reference)

              Ok(Json.obj("token" -> uuid))
                .withCookies(
                  Cookie(AuthTokenCookieKey, uuid, None, httpOnly = false))
          }
      }
    }

  /** Log-out a user. Invalidates the authentication token.
    *
    * Discard the cookie [[AuthTokenCookieKey]] to have AngularJS no longer set
    * the X-XSRF-TOKEN in HTTP header.
    */
  def logout(): Action[Unit] =
    HasToken(parse.empty, withinTransaction = false) {
      _ => subject => implicit request =>
        doLogout(subject);
    }

  private def doLogout(subject: Subject): Future[Result] = {
    logger.debug(s"Remove token from cache: ${subject.token}")
    cache.remove(subject.token)

    systemServices.loginStateAggregate ! UserLoggedOutV2(subject.userReference)

    // notify client
    clientReceiver ! (subject.userReference.id, UserLoggedOutV2(
      subject.userReference), List(subject.userReference.id))

    Future.successful(
      Ok.discardingCookies(DiscardingCookie(name = AuthTokenCookieKey)))
  }

  /** Load application config
    */
  def getConfig: Action[AnyContent] = Action.async {
    Future.successful(Ok(Json.toJson(appConfig)))
  }
}
