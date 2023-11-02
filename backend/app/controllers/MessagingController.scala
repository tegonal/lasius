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

import actors.ClientMessagingWebsocketActor
import akka.actor.ActorSystem
import akka.stream.{Materializer, OverflowStrategy}
import akka.util.Timeout
import controllers.MessagingController.AuthOneTimeTokenQueryParamKey
import core.SystemServices
import models._
import org.pac4j.core.context.session.SessionStore
import org.pac4j.play.scala.SecurityComponents
import play.api.cache.{AsyncCacheApi, NamedCache}
import play.api.libs.json._
import play.api.libs.streams._
import play.api.mvc._
import play.modules.reactivemongo.ReactiveMongoApi
import play.sockjs.api._

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.Future.successful
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class MessagingController @Inject() (
    override val controllerComponents: SecurityComponents,
    override val authConfig: AuthConfig,
    override val systemServices: SystemServices,
    @NamedCache("one-time-tokens") val oneTimeAccessTokenCache: AsyncCacheApi,
    override val playSessionStore: SessionStore)(implicit
    executionContext: ExecutionContext,
    override val reactiveMongoApi: ReactiveMongoApi)
    extends BaseLasiusController(controllerComponents)
    with InjectedSockJSRouter {

  implicit val timeout: Timeout = systemServices.timeout

  implicit val system: ActorSystem        = systemServices.system
  implicit val materializer: Materializer = Materializer.matFromSystem

  implicit val sockJsMessageFlowTransformer
      : SockJS.MessageFlowTransformer[InEvent, OutEvent] =
    SockJS.MessageFlowTransformer.jsonMessageFlowTransformer[InEvent, OutEvent]

  def sockjs: SockJS = SockJS.acceptOrResult[InEvent, OutEvent](messageHandler)

  private val messageHandler = { implicit request: RequestHeader =>
    withDBSession() { implicit dbSession =>
      checkOneTimeAuthToken().map {
        case Left(result) =>
          logger.warn(
            s"Couldn't create Websocket for client ${result.header} - ${result.body}")
          Left(result)
        case Right(subject) =>
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
  }

  def acquireOneTimeToken(): Action[Unit] =
    HasToken(parse.empty, withinTransaction = false) {
      implicit dbSession => implicit subject => implicit request =>
        val newToken = UUID.randomUUID().toString
        oneTimeAccessTokenCache.set(newToken, subject, 1 minute)
        Future.successful(Ok(Json.toJson(OneTimeToken(newToken))))
    }

  /** check auth token by validating xsrf-token cookie with xsrf-token provided
    * either in header or as query parameter used for websocket authentication.
    * Perform user reference lookup within cache or db
    */
  def checkOneTimeAuthToken()(implicit
      request: RequestHeader): Future[Either[Result, Subject[_]]] = {
    val maybeOneTimeToken =
      request.getQueryString(AuthOneTimeTokenQueryParamKey)
    logger.debug("One-time token from headers:" + maybeOneTimeToken)
    maybeOneTimeToken
      .map { oneTimeToken =>
        oneTimeAccessTokenCache
          .get[Subject[_]](oneTimeToken)
          .flatMap { maybeSubject =>
            oneTimeAccessTokenCache.remove(oneTimeToken).map { _ =>
              maybeSubject.toRight(
                Unauthorized(Json.obj("message" -> "No Token found")))
            }
          }
      }
      .getOrElse {
        successful(
          Left(Unauthorized(Json.obj("message" -> "No Token provided"))))
      }
  }
}

object MessagingController {
  val AuthOneTimeTokenQueryParamKey = "otoken"
}
