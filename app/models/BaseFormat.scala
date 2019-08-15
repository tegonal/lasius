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
package models

import java.net.{URI, URL}

import com.tegonal.play.json.TypedId._
import models.BaseFormat.CompositeBaseId
import org.joda.time.{DateTime, Duration}
import play.api.libs.json._
import reactivemongo.bson.{BSONDateTime, BSONHandler, BSONObjectID}

import scala.util.Success

trait BaseEntity[I <: BaseId[_]] {
  val id: I
}

object BaseFormat {
  trait CompositeBaseId[I1, I2] extends BaseId[((String, I1), (String, I2))]

  trait BaseBSONObjectId extends BaseId[BSONObjectID]

  //extended format function
  def idformat[I <: BaseBSONObjectId](implicit fact: Factory[BSONObjectID, I]) = new BSONObjectIdTypedIdFormat[I]
  def idformat[I <: CompositeBaseId[I1, I2], I1, I2](implicit fact: (I1, I2) => I, f: Format[I1], ff2: Format[I2]) = new CompositeIdTypedIdFormat[I, I1, I2]

  implicit object URIFormat extends Format[URI] {
    def writes(uri: URI): JsValue = {
      JsString(uri.toURL().toExternalForm())
    }
    def reads(json: JsValue): JsResult[URI] = json match {
      case JsString(x) => {
        JsSuccess(new URI(x))
      }
      case _ => JsError("Expected URI as JsString")
    }
  }
  
  implicit val durationFormat: Format[Duration] = new Format[Duration] {
    def reads(json: JsValue): JsResult[Duration] = json match {

      case JsNumber(millis) => {
        JsSuccess(Duration.millis(millis.toLong))
      }
      case _ => JsError(s"Unexpected JSON value $json")
    }

    def writes(duration: Duration): JsValue = JsNumber(duration.getMillis)
  }
  
  implicit val urlFormat: Format[URL] = new Format[URL] {
    def reads(json: JsValue): JsResult[URL] = json match {

      case JsString(url) => {
        try {
          JsSuccess(new URL(url))
        }
        catch {
          case e:Throwable => 
            JsError(s"couldn't parse url:$url, $e")
        }
      }
      case _ => JsError(s"Unexpected JSON value $json")
    }

    def writes(url: URL): JsValue = JsString(url.toString)
  }

  implicit object BSONDateTimeHandler extends BSONHandler[BSONDateTime, DateTime] {
    def read(time: BSONDateTime) = new DateTime(time.value)
    def write(jdtime: DateTime) = BSONDateTime(jdtime.getMillis)
  }

  val defaultTypeFormat = (__ \ "type").format[String]

  val dateTimePattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ"
  implicit val dateFormat = Format[DateTime](JodaReads.jodaDateReads(dateTimePattern), JodaWrites.jodaDateWrites(dateTimePattern))
}

class BSONObjectIdTypedIdFormat[I <: BaseId[BSONObjectID]](implicit fact: Factory[BSONObjectID, I]) extends Format[I] {

  def writes(objectId: I): JsValue = {
    Json.obj("$oid" -> JsString(objectId.value.stringify))
  }

  def reads(json: JsValue): JsResult[I] = json match {
    case JsString(value) => {
      BSONObjectID.parse(value) match {
        case Success(id) =>
          JsSuccess(fact(id))
        case _ =>
          JsError(s"Unexpected JSON value $json")
      }
    }
    case JsObject(maps) =>
      maps.get("$oid").map { value =>
        reads(value)
      }.getOrElse(JsError(s"Unexpected JSON value $maps"))
    case _ => JsError(s"Unexpected JSON value $json")
  }
}

class CompositeIdTypedIdFormat[I <: CompositeBaseId[I1, I2], I1, I2](implicit fact: (I1, I2) => I, f1: Format[I1], f2: Format[I2]) extends Format[I] {
  def reads(json: JsValue): JsResult[I] = json match {
    case JsObject(values) => {
      Json.fromJson[I1](values.toSeq(0)._2) match {
        case JsSuccess(i1, _) =>
          Json.fromJson[I2](values.toSeq(1)._2) match {
            case JsSuccess(i2, _) =>
              JsSuccess(fact(i1, i2))
            case _ => JsError(s"Unexpected JSON value $json")
          }
        case _ => JsError(s"Unexpected JSON value $json")
      }

    }
    case _ => JsError(s"Unexpected JSON value $json")
  }

  def writes(id: I): JsValue = {
    Json.obj(id.value._1._1 -> Json.toJson[I1](id.value._1._2), id.value._2._1 -> Json.toJson[I2](id.value._2._2))
  }
}