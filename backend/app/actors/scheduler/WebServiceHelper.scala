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

package actors.scheduler

import java.io.IOException

import org.apache.commons.codec.binary.Base64
import play.api.libs.json.JsValue
import play.api.libs.oauth._
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object WebServiceHelper {

  def call(wsClient: WSClient, config: ServiceConfiguration, url: String)(
      implicit
      auth: ServiceAuthentication,
      executionContext: ExecutionContext)
      : Future[Try[(JsValue, Map[String, scala.collection.Seq[String]])]] = {
    auth match {
      case apiKey: ApiKeyAuthentication =>
        callWithApiKey(wsClient, config, url, apiKey)
      case oauth: OAuthAuthentication =>
        callWithOAuth(wsClient, config, url, oauth)
      case oauth2: OAuth2Authentication =>
        callWithOAuth2(wsClient, config, url, oauth2)
      case basicAuth: BasicAuthentication =>
        callWithBasicAuth(wsClient, config, url, basicAuth)
    }
  }

  def oauth(config: ServiceConfiguration, auth: OAuthAuthentication): OAuth = {
    val KEY = ConsumerKey(auth.consumerKey, auth.privateKey)
    OAuth(
      ServiceInfo(
        config.baseUrl + "/plugins/servlet/oauth/request-token",
        config.baseUrl + "/plugins/servlet/oauth/access-token",
        config.baseUrl + "/plugins/servlet/oauth/authorize",
        KEY
      ),
      use10a = true
    )
  }

  def callWithApiKey(wsClient: WSClient,
                     config: ServiceConfiguration,
                     url: String,
                     auth: ApiKeyAuthentication)(implicit
      executionContext: ExecutionContext)
      : Future[Try[(JsValue, Map[String, scala.collection.Seq[String]])]] = {
    wsClient
      .url(url)
      .addHttpHeaders(s"x-api-key" -> s"${auth.apiKey}")
      .get()
      .map { result =>
        result.status match {
          case 200 => Success((result.json, result.headers))
          case error =>
            Failure(new IOException(s"Http status:$error:${result.statusText}"))
        }
      }
  }

  def callWithOAuth(wsClient: WSClient,
                    config: ServiceConfiguration,
                    url: String,
                    auth: OAuthAuthentication)(implicit
      executionContext: ExecutionContext)
      : Future[Try[(JsValue, Map[String, scala.collection.Seq[String]])]] = {
    wsClient
      .url(url)
      .sign(OAuthCalculator(ConsumerKey(auth.consumerKey, auth.privateKey),
                            RequestToken(auth.token, auth.tokenSecret)))
      .get()
      .map { result =>
        result.status match {
          case 200 => Success((result.json, result.headers))
          case error =>
            Failure(new IOException(s"Http status:$error:${result.statusText}"))
        }
      }
  }

  def callWithOAuth2(wsClient: WSClient,
                     config: ServiceConfiguration,
                     url: String,
                     auth: OAuth2Authentication)(implicit
      executionContext: ExecutionContext)
      : Future[Try[(JsValue, Map[String, scala.collection.Seq[String]])]] = {
    wsClient
      .url(url)
      .addHttpHeaders(s"Authorization" -> s"Bearer ${auth.token}")
      .get()
      .map { result =>
        result.status match {
          case 200 => Success((result.json, result.headers))
          case error =>
            Failure(
              new IOException(
                s"Http status:$error:${result.statusText}, url:$url"))
        }
      }

  }

  def callWithBasicAuth(ws: WSClient,
                        config: ServiceConfiguration,
                        url: String,
                        auth: BasicAuthentication)(implicit
      executionContext: ExecutionContext)
      : Future[Try[(JsValue, Map[String, scala.collection.Seq[String]])]] = {
    val pair = s"${auth.username}:${auth.password}"
    val encPart =
      new String(Base64.encodeBase64(pair.getBytes("utf-8")), "utf-8")
    val enc = s"Basic $encPart"

    ws.url(url)
      .addHttpHeaders(headers() :+ ("Authorization" -> enc): _*)
      .get()
      .map { resp =>
        resp.status match {
          case 200   => Success((resp.json, resp.headers))
          case error => Failure(new IOException(s"Http status:$error"))
        }
      }
  }

  def headers(): Seq[(String, String)] = {
    Seq("Content-Type" -> "application/json")
  }
}
