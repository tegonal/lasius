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
package actors

import akka.serialization._
import play.api.libs.json._
import play.api.Logger
import models._

class JsonSerializer extends Serializer {
  // This is whether "fromBinary" requires a "clazz" or not
  def includeManifest: Boolean = true

  val charset = "UTF-8"

  // Pick a unique identifier for your Serializer,
  // you've got a couple of billions to choose from,
  // 0 - 16 is reserved by Akka itself
  def identifier = 523452349

  // "toBinary" serializes the given object to an Array of Bytes
  def toBinary(obj: AnyRef): Array[Byte] = {
    if (obj.isInstanceOf[PersistetEvent]) {
      val event = obj.asInstanceOf[PersistetEvent]
      Json.toJson(event).toString.getBytes(charset);
    } else {
      Array.empty
    }
  }

  def isAssignableFrom[T: Manifest](c: Class[_]) =
    manifest[T].runtimeClass.isAssignableFrom(c)

  // "fromBinary" deserializes the given array,
  // using the type hint (if any, see "includeManifest" above)
  def fromBinary(bytes: Array[Byte],
    clazz: Option[Class[_]]): AnyRef = {

    val r = clazz.map { cl =>
      if (isAssignableFrom[PersistetEvent](cl)) {
        val json = Json.parse(new String(bytes, charset))
        Json.fromJson[PersistetEvent](json) match {
          case JsSuccess(obj, _) => obj
          case JsError(e) =>
            Logger.error(s"Coulnd't convert json value $json")
            UndefinedEvent
        }
      }
    }.getOrElse {
      Logger.error("missing manifest")
      UndefinedEvent
    }
    r.asInstanceOf[AnyRef]
  }

}

