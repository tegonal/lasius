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

package core.swagger

import com.iheart.playSwagger.OutputTransformer
import core.swagger.SwaggerRenameModelClassesTransformer.customModelNames
import models._
import play.api.libs.json.{JsObject, JsString}

import scala.util.{Success, Try}

class SwaggerRenameModelClassesTransformer extends OutputTransformer {
  override def apply(obj: JsObject): Try[JsObject] = Success(tf(obj))

  private def tf(obj: JsObject): JsObject = JsObject {
    obj.fields.map {
      case (key, value: JsObject) => (renameModel(key), tf(value))
      case (key, JsString(value)) =>
        (renameModel(key), JsString(renameModel(value)))
      case (key, other) => (renameModel(key), other)
      case e            => e
    }
  }

  private final val renameModel: String => String = { case className =>
    customModelNames.find { case (k, _) =>
      className.endsWith(k.getName)
    } match {
      case Some((k, v)) => className.replace(k.getName, v)
      case None         => className
    }
  }
}

object SwaggerRenameModelClassesTransformer {
  val customModelNames: Map[Class[_], String] = Map(
    classOf[BookingV2] -> "models.Booking",
    classOf[UserDTO]   -> "models.User")
}
