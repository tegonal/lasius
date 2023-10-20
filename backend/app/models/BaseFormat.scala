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

package models

import com.tegonal.play.json.TypedId._
import julienrf.json.derived
import julienrf.json.derived.{DerivedReads, TypeTag}
import models.BaseFormat.CompositeBaseId
import org.joda.time.format.DateTimeFormat
import org.joda.time._
import play.api.libs.json._
import reactivemongo.api.bson._
import shapeless.Lazy

import java.net.{URI, URL}
import java.util.UUID
import scala.util.Success

trait BaseEntity[I <: BaseId[_]] {
  val id: I
}

case class EntityReference[I <: BaseId[_]](id: I, key: String)

object EntityReference {
  implicit def entityReferenceFormat[I <: BaseId[_]](implicit
      idFormat: Format[I]): Format[EntityReference[I]] =
    Json.format[EntityReference[I]]
}

object BaseFormat {

  trait CompositeBaseId[I1, I2] extends BaseId[((String, I1), (String, I2))]

  trait UUIDBaseId extends BaseId[UUID]

  trait BaseBSONObjectId extends BaseId[BSONObjectID]

  // extended format function
  def idformat[I <: BaseBSONObjectId](implicit fact: Factory[BSONObjectID, I]) =
    new BSONObjectIdTypedIdFormat[I](fact, _.value)

  def idformat[I <: UUIDBaseId](implicit fact: Factory[UUID, I]) =
    new StringBasedTypedIdFormat[I](str => fact(UUID.fromString(str)),
                                    _.value.toString)

  def idformat[I <: CompositeBaseId[I1, I2], I1, I2](implicit
      fact: (I1, I2) => I,
      f: Format[I1],
      ff2: Format[I2]) = new CompositeIdTypedIdFormat[I, I1, I2]

  implicit object URIFormat extends Format[URI] {
    def writes(uri: URI): JsValue = JsString(uri.toURL.toExternalForm)

    def reads(json: JsValue): JsResult[URI] = json match {
      case JsString(x) =>
        JsSuccess(new URI(x))
      case _ => JsError("Expected URI as JsString")
    }
  }

  implicit val durationFormat: Format[Duration] = new Format[Duration] {
    def reads(json: JsValue): JsResult[Duration] = json match {

      case JsNumber(millis) =>
        JsSuccess(Duration.millis(millis.toLong))
      case _ => JsError(s"Unexpected JSON value $json")
    }

    def writes(duration: Duration): JsValue = JsNumber(duration.getMillis)
  }

  implicit val urlFormat: Format[URL] = new Format[URL] {
    def reads(json: JsValue): JsResult[URL] = json match {

      case JsString(url) =>
        try {
          JsSuccess(new URL(url))
        } catch {
          case e: Throwable =>
            JsError(s"couldn't parse url:$url, $e")
        }
      case _ => JsError(s"Unexpected JSON value $json")
    }

    def writes(url: URL): JsValue = JsString(url.toString)
  }

  implicit object BSONDateTimeHandler {

    import reactivemongo.api.bson._

    implicit val dateTimeReader: BSONReader[DateTime] =
      BSONReader.from[DateTime] { bson =>
        bson.asTry[BSONDateTime].map(time => new DateTime(time.value))
      }

    implicit val dateTimeWriter: BSONWriter[DateTime] =
      BSONWriter.from[DateTime] { javaDateTime =>
        scala.util.Success(BSONDateTime(javaDateTime.getMillis))
      }
  }

  val defaultTypeFormat: OFormat[String] = (__ \ "type").format[String]

  private val selfTypeFormat: OFormat[String] = __.format[String]

  private def toStringWrites[T]: Writes[T] =
    Writes[T](obj => JsString(obj.toString))

  def enumFormat[T](implicit
      derivedReads: Lazy[DerivedReads[T, TypeTag.ShortClassName]]
  ): Format[T] = {
    implicit val reads: Reads[T]   = derived.flat.reads(selfTypeFormat)
    implicit val writes: Writes[T] = toStringWrites[T]

    Format[T](reads, writes)
  }

  val dateTimePattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ"
  implicit val dateFormat: Format[DateTime] = Format[DateTime](
    JodaReads.jodaDateReads(dateTimePattern),
    JodaWrites.jodaDateWrites(dateTimePattern))
  implicit val optionDateFormat: Format[Option[DateTime]] =
    Format.optionWithNull

  val localDateTimePattern = "yyyy-MM-dd'T'HH:mm:ss.SSS"
  private val localDateTimeReads: Reads[LocalDateTime] = {
    new Reads[LocalDateTime] {
      private val df =
        DateTimeFormat.forPattern(localDateTimePattern)

      def reads(json: JsValue): JsResult[LocalDateTime] = json match {
        case JsString(s) =>
          parseDate(s) match {
            case Some(d) => JsSuccess(d)
            case _ =>
              JsError(
                Seq(
                  JsPath() -> Seq(
                    JsonValidationError("error.expected.jodadatetime.format",
                                        localDateTimePattern))))
          }
        case _ =>
          JsError(
            Seq(JsPath() -> Seq(JsonValidationError("error.expected.date"))))
      }

      private def parseDate(input: String): Option[LocalDateTime] =
        scala.util.control.Exception
          .nonFatalCatch[LocalDateTime]
          .opt(LocalDateTime.parse(input, df))
    }
  }
  private val localDateTimeWrites: Writes[LocalDateTime] = {
    val df =
      org.joda.time.format.DateTimeFormat.forPattern(localDateTimePattern)
    Writes[LocalDateTime] { d =>
      JsString(d.toString(df))
    }
  }
  implicit val localDateTimeFormat: Format[LocalDateTime] =
    Format[LocalDateTime](localDateTimeReads, localDateTimeWrites)

  val localDatePattern = "yyyy-MM-dd"
  private val localDateReads: Reads[LocalDate] = {
    new Reads[LocalDate] {
      private val df =
        DateTimeFormat.forPattern(localDatePattern)

      def reads(json: JsValue): JsResult[LocalDate] = json match {
        case JsString(s) =>
          parseDate(s) match {
            case Some(d) => JsSuccess(d)
            case _ =>
              JsError(
                Seq(
                  JsPath() -> Seq(
                    JsonValidationError("error.expected.jodadatetime.format",
                                        localDatePattern))))
          }
        case _ =>
          JsError(
            Seq(JsPath() -> Seq(JsonValidationError("error.expected.date"))))
      }

      private def parseDate(input: String): Option[LocalDate] =
        scala.util.control.Exception
          .nonFatalCatch[LocalDate]
          .opt(LocalDate.parse(input, df))
    }
  }
  private val localDateWrites: Writes[LocalDate] = {
    val df =
      org.joda.time.format.DateTimeFormat.forPattern(localDatePattern)
    Writes[LocalDate] { d =>
      JsString(d.toString(df))
    }
  }
  implicit val localDateFormat: Format[LocalDate] =
    Format[LocalDate](localDateReads, localDateWrites)

  implicit object DateTimeZoneFormat extends Format[DateTimeZone] {
    def writes(zone: DateTimeZone): JsValue = {
      JsString(zone.getID)
    }

    def reads(json: JsValue): JsResult[DateTimeZone] = json match {
      case JsString(id) =>
        JsSuccess(DateTimeZone.forID(id))
      case _ => JsError("Expected DateTimeZone id as JsString")
    }
  }

  implicit object NumberFormat extends Format[Number] {
    def writes(nb: Number): JsValue = {
      JsNumber(BigDecimal(nb.doubleValue()))
    }

    def reads(json: JsValue): JsResult[Number] = json match {
      case JsNumber(x) =>
        JsSuccess(x.toDouble)
      case _ => JsError("Expected URI as JsString")
    }
  }

  val plainBSONObjectIDFormat: Format[BSONObjectID] =
    new BSONObjectIdTypedIdFormat[BSONObjectID](identity, identity)
}

class BSONObjectIdTypedIdFormat[I](toObjectFactory: BSONObjectID => I,
                                   fromObjectFactory: I => BSONObjectID)
    extends Format[I] {

  def writes(objectId: I): JsValue = {
    Json.obj("$oid" -> JsString(fromObjectFactory(objectId).stringify))
  }

  def reads(json: JsValue): JsResult[I] = json match {
    case JsString(value) =>
      BSONObjectID.parse(value) match {
        case Success(id) =>
          JsSuccess(toObjectFactory(id))
        case _ =>
          JsError(s"Unexpected JSON value $json")
      }
    case JsObject(maps) =>
      maps
        .get("$oid")
        .map { value =>
          reads(value)
        }
        .getOrElse(JsError(s"Unexpected JSON value $maps"))
    case _ => JsError(s"Unexpected JSON value $json")
  }
}

class StringBasedTypedIdFormat[I](fromString: String => I,
                                  toString: I => String)
    extends Format[I] {
  def reads(json: JsValue): JsResult[I] = json match {

    case JsString(value) => JsSuccess(fromString(value))
    case _               => JsError(s"Unexpected JSON value $json")
  }

  def writes(id: I): JsValue = JsString(toString(id))
}

class CompositeIdTypedIdFormat[I <: CompositeBaseId[I1, I2], I1, I2](implicit
    fact: (I1, I2) => I,
    f1: Format[I1],
    f2: Format[I2])
    extends Format[I] {
  def reads(json: JsValue): JsResult[I] = json match {
    case JsObject(values) =>
      Json.fromJson[I1](values.toSeq.head._2) match {
        case JsSuccess(i1, _) =>
          Json.fromJson[I2](values.toSeq(1)._2) match {
            case JsSuccess(i2, _) =>
              JsSuccess(fact(i1, i2))
            case _ => JsError(s"Unexpected JSON value $json")
          }
        case _ => JsError(s"Unexpected JSON value $json")
      }
    case _ => JsError(s"Unexpected JSON value $json")
  }

  def writes(id: I): JsValue = {
    Json.obj(id.value._1._1 -> Json.toJson[I1](id.value._1._2),
             id.value._2._1 -> Json.toJson[I2](id.value._2._2))
  }
}
