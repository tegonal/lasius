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

import java.net.URLEncoder

import play.api.Logging
import play.api.libs.json.{JsArray, Json, Reads}
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait ApiServiceBase extends Logging {
  val ws: WSClient
  val config: ServiceConfiguration

  def getParamList(params: Option[String]*): String = {
    params.flatten.mkString("&")
  }

  def getParam[T](name: String, value: T): Option[String] = {
    getParam(name, Some(value))
  }

  def getParam[T](name: String, value: Option[T]): Option[String] = {
    value.map(v => name + "=" + URLEncoder.encode(v.toString, "UTF-8"))
  }

  def getList[T](relUrl: String)(implicit
      auth: ServiceAuthentication,
      executionContext: ExecutionContext,
      reads: Reads[T])
      : Future[(Seq[T], Map[String, scala.collection.Seq[String]])] = {
    val url = config.baseUrl + relUrl
    logger.debug(s"getList(url:$url")
    WebServiceHelper.call(ws, config, url).flatMap {
      case Success((json: JsArray, headers)) =>
        logger.debug(s"getList:Success (JsArray) -> $json")
        Json
          .fromJson[Seq[T]](json)
          .asEither match {
          case Right(j) => Future.successful((j, headers))
          case Left(jsError) =>
            logger.error(s"Couldn't parse json:$jsError")
            Future.failed(new RuntimeException(s"Could not parse $json"))
        }
      case Success((json, headers)) =>
        logger.debug(s"getList:Success (json) -> $json")
        Json
          .fromJson[T](json)
          .asEither match {
          case Right(j) => Future.successful((Seq(j), headers))
          case Left(jsError) =>
            logger.error(s"Couldn't parse json:$jsError")
            Future.failed(new RuntimeException(s"Could not parse $json"))
        }
      case Failure(e) =>
        logger.debug(s"getList:Failure -> $e")
        Future.failed(e)
    }
  }

  def getSingleValue[T](relUrl: String)(implicit
      auth: ServiceAuthentication,
      executionContext: ExecutionContext,
      reads: Reads[T])
      : Future[(T, Map[String, scala.collection.Seq[String]])] = {
    val url = config.baseUrl + relUrl
    logger.debug(s"getSingleValue(url:$url")
    WebServiceHelper.call(ws, config, url).flatMap {
      case Success((json, headers)) =>
        logger.debug(s"getOption:Success (json) -> $json")
        Json
          .fromJson[T](json)
          .asEither match {
          case Right(j) => Future.successful((j, headers))
          case Left(jsError) =>
            logger.error(s"Couldn't parse json:$jsError")
            Future.failed(new RuntimeException(s"Could not parse $json"))
        }
      case Failure(e) =>
        logger.debug(s"getOption:Failure -> $e")
        Future.failed(e)
    }
  }
}
