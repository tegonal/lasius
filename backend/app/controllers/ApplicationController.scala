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

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.util.Timeout
import com.typesafe.config.Config
import core.SystemServices
import org.pac4j.core.context.session.SessionStore
import org.pac4j.play.scala.SecurityComponents
import play.api.libs.json._
import play.api.mvc._
import play.modules.reactivemongo.ReactiveMongoApi

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

case class LoginForm(email: String, password: String)

object LoginForm {
  implicit val loginFormFormat: OFormat[LoginForm] = Json.format[LoginForm]
}

class ApplicationController @Inject() (
    override val controllerComponents: SecurityComponents,
    override val authConfig: AuthConfig,
    override val systemServices: SystemServices,
    override val playSessionStore: SessionStore)(implicit
    executionContext: ExecutionContext,
    override val reactiveMongoApi: ReactiveMongoApi)
    extends BaseLasiusController(controllerComponents) {

  implicit val timeout: Timeout = systemServices.timeout

  implicit val system: ActorSystem        = systemServices.system
  implicit val materializer: Materializer = Materializer.matFromSystem

  systemServices.initialize()

  /** Load application config
    */
  def getConfig: Action[AnyContent] = Action.async {
    Future.successful(Ok(Json.toJson(systemServices.appConfig)))
  }
}
