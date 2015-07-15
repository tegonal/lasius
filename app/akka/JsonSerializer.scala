package akka

import akka.actor.{ ActorRef, ActorSystem }
import akka.serialization._
import com.typesafe.config.ConfigFactory
import domain.AggregateRoot
import play.api.libs.json._
import julienrf.variants.Variants
import models.PersistetEvent
import play.api.Logger
import models.UndefinedEvent

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

