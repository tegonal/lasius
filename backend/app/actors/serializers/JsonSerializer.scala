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

package actors.serializers

import akka.serialization._
import models._
import play.api.Logging
import play.api.libs.json._
import utils.GzipCompression

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.util.zip.{GZIPInputStream, GZIPOutputStream}
import scala.util.Try

abstract class JsonSerializer[C <: AnyRef: Format](implicit mf: Manifest[C])
    extends SerializerWithStringManifest
    with Logging
    with GzipCompression {

  private val charset       = "UTF-8"
  private val eventManifest = mf.runtimeClass.getName

  def manifest(obj: AnyRef): String =
    obj match {
      case _: C => eventManifest
    }

  // "toBinary" serializes the given object to an Array of Bytes
  def toBinary(obj: AnyRef): Array[Byte] = {
    obj match {
      case event: C => compress(Json.toJson(event).toString.getBytes(charset))
      case _        => Array.empty
    }
  }

  def isAssignableFrom(className: String): Boolean = {
    Try(mf.runtimeClass.isAssignableFrom(Class.forName(className)))
      .fold(_ => false, identity)
  }

  // "fromBinary" deserializes the given array,
  // using the type hint (if any, see "includeManifest" above)
  def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = {
    // use isAssignableFrom to support old messages storing full classname
    // of event as manifest
    if (acceptManifest(manifest)) {
      val uncompressedContent = decompress(bytes)
      val json = Json.parse(new String(uncompressedContent, charset))
      Json.fromJson[C](json) match {
        case JsSuccess(obj, _) => obj
        case JsError(e) =>
          logger.error(s"Couldn't convert json value $json: $e")
          UndefinedEvent
      }
    } else {
      logger.error(s"unknown manifest: $manifest")
      UndefinedEvent
    }
  }

  protected def acceptManifest(manifest: String) = isAssignableFrom(manifest)
}
