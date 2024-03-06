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

package binders

import com.tegonal.play.json.TypedId._
import models.BaseFormat.{
  dateTimePattern,
  localDatePattern,
  localDateTimePattern,
  BaseBSONObjectId,
  UUIDBaseId
}
import models._
import org.joda.time._
import org.joda.time.format._
import play.api.libs.json._
import play.api.mvc._
import reactivemongo.api.bson.BSONObjectID

import java.text.{ParseException, SimpleDateFormat}
import java.util.UUID
import scala.reflect.ClassTag
import scala.util._

object Binders {

  implicit def OptionBindable[T: PathBindable]: PathBindable[Option[T]] =
    new PathBindable[Option[T]] {
      override def bind(key: String, value: String): Either[String, Option[T]] =
        implicitly[PathBindable[T]]
          .bind(key, value)
          .fold(left => Left(left), right => Right(Some(right)))

      override def unbind(key: String, value: Option[T]): String =
        value.map(_.toString).getOrElse("")
    }

  implicit def stringBaseIdPathBindable[T <: StringBaseId](implicit
      stringBinder: PathBindable[String],
      fact: String => T): PathBindable[T] = new PathBindable[T] {

    override def bind(key: String, value: String): Either[String, T] =
      for {
        id <- stringBinder.bind(key, value)
      } yield fact(id)

    override def unbind(key: String, id: T): String =
      stringBinder.unbind(key, id.value)
  }

  implicit def bsonObjectIdBaseIdPathBindable[T <: BaseBSONObjectId](implicit
      stringBinder: PathBindable[String],
      fact: BSONObjectID => T): PathBindable[T] = new PathBindable[T] {

    override def bind(key: String, value: String): Either[String, T] =
      stringBinder.bind(key, value) match {
        case Right(id) =>
          BSONObjectID.parse(id) match {
            case Success(bsonid) =>
              Right(fact(bsonid))
            case Failure(f) =>
              Left(f.toString)
          }
        case Left(s) =>
          Left(s)
      }

    override def unbind(key: String, id: T): String =
      stringBinder.unbind(key, id.value.toString)
  }

  implicit def uuidBaseIdPathBindable[T <: UUIDBaseId](implicit
      stringBinder: PathBindable[String],
      fact: UUID => T): PathBindable[T] = new PathBindable[T] {

    override def bind(key: String, value: String): Either[String, T] =
      stringBinder.bind(key, value) match {
        case Right(id) =>
          Try(UUID.fromString(id)) match {
            case Success(uuid) =>
              Right(fact(uuid))
            case Failure(f) =>
              Left(f.toString)
          }
        case Left(s) =>
          Left(s)
      }

    override def unbind(key: String, id: T): String =
      stringBinder.unbind(key, id.value.toString)
  }

  implicit def numberBaseIdPathBindable[T <: NumberBaseId](implicit
      numberBinder: PathBindable[Number],
      fact: Number => T): PathBindable[T] = new PathBindable[T] {

    override def bind(key: String, value: String): Either[String, T] =
      for {
        id <- numberBinder.bind(key, value)
      } yield fact(id)

    override def unbind(key: String, id: T): String =
      numberBinder.unbind(key, id.value)
  }

  implicit def stringBaseIdQueryStringBinder[T <: StringBaseId](implicit
      strBinder: QueryStringBindable[String],
      fact: String => T): QueryStringBindable[T] = new QueryStringBindable[T] {
    override def bind(
        key: String,
        params: Map[String, Seq[String]]): Option[Either[String, T]] = {
      for {
        str <- strBinder.bind(key, params)
      } yield {
        str match {
          case Right(id) =>
            Right(fact(id))
          case _ => Left("Unable to bind id")
        }
      }
    }

    override def unbind(key: String, id: T): String = {
      strBinder.unbind(key, id.value)
    }
  }

  implicit def bsonObjectIdBaseIdQueryStringBinder[T <: BaseBSONObjectId](
      implicit
      strBinder: QueryStringBindable[String],
      fact: BSONObjectID => T): QueryStringBindable[T] =
    new QueryStringBindable[T] {
      override def bind(
          key: String,
          params: Map[String, Seq[String]]): Option[Either[String, T]] = {
        for {
          str <- strBinder.bind(key, params)
        } yield {
          str match {
            case Right(id) =>
              BSONObjectID.parse(id) match {
                case Success(bsonid) =>
                  Right(fact(bsonid))
                case Failure(f) =>
                  Left(f.toString)
              }
            case _ =>
              Left("Unable to bind id")
          }
        }
      }

      override def unbind(key: String, id: T): String = {
        strBinder.unbind(key, id.value.toString)
      }
    }

  implicit def uuidBaseIdQueryStringBinder[T <: UUIDBaseId](implicit
      strBinder: QueryStringBindable[String],
      fact: UUID => T): QueryStringBindable[T] =
    new QueryStringBindable[T] {
      override def bind(
          key: String,
          params: Map[String, Seq[String]]): Option[Either[String, T]] = {
        for {
          str <- strBinder.bind(key, params)
        } yield {
          str match {
            case Right(id) =>
              Try(UUID.fromString(id)) match {
                case Success(uuid) =>
                  Right(fact(uuid))
                case Failure(f) =>
                  Left(f.toString)
              }
            case _ =>
              Left("Unable to bind id")
          }
        }
      }

      override def unbind(key: String, id: T): String = {
        strBinder.unbind(key, id.value.toString)
      }
    }

  implicit def numberBaseIdQueryStringBinder[T <: NumberBaseId](implicit
      strBinder: QueryStringBindable[Number],
      fact: Number => T): QueryStringBindable[T] = new QueryStringBindable[T] {
    override def bind(
        key: String,
        params: Map[String, Seq[String]]): Option[Either[String, T]] = {
      for {
        str <- strBinder.bind(key, params)
      } yield {
        str match {
          case Right(id) =>
            Right(fact(id))
          case _ => Left("Unable to bind id")
        }
      }
    }

    override def unbind(key: String, id: T): String = {
      strBinder.unbind(key, id.value)
    }
  }

  implicit def dateTimeQueryStringBinder(implicit
      strBinder: QueryStringBindable[String]): QueryStringBindable[DateTime] =
    new QueryStringBindable[DateTime] {
      override def bind(key: String, params: Map[String, Seq[String]])
          : Option[Either[String, DateTime]] = {
        for {
          dateStr <- strBinder.bind(key, params)
        } yield {
          dateStr match {
            case Right(dateStr) =>
              val fmt = DateTimeFormat.forPattern(dateTimePattern)
              Try(fmt.parseDateTime(dateStr)) match {
                case Success(result) => Right(result)
                case Failure(e) =>
                  Left(
                    "Cannot parse parameter " + key + " as small DateTime: " + e.getMessage)
              }
            case _ => Left("Unable to bind DateTime")
          }
        }
      }

      override def unbind(key: String, value: DateTime): String = {
        val fmt = DateTimeFormat.forPattern(dateTimePattern)
        strBinder.unbind(key, fmt.print(value))
      }

    }

  implicit def localDateTimeQueryStringBinder(implicit
      strBinder: QueryStringBindable[String])
      : QueryStringBindable[LocalDateTime] =
    new QueryStringBindable[LocalDateTime] {
      override def bind(key: String, params: Map[String, Seq[String]])
          : Option[Either[String, LocalDateTime]] = {
        for {
          dateStr <- strBinder.bind(key, params)
        } yield {
          dateStr match {
            case Right(dateStr) =>
              val fmt = DateTimeFormat.forPattern(localDateTimePattern)
              Try(fmt.parseDateTime(dateStr)) match {
                case Success(result) => Right(result.toLocalDateTime)
                case Failure(e) =>
                  Left(
                    "Cannot parse parameter " + key + " as small LocalDateTime: " + e.getMessage)
              }
            case _ => Left("Unable to bind LocalDateTime")
          }
        }
      }

      override def unbind(key: String, value: LocalDateTime): String = {
        val fmt = DateTimeFormat.forPattern(localDateTimePattern)
        strBinder.unbind(key, fmt.print(value))
      }

    }

  implicit def localDateQueryStringBinder(implicit
      strBinder: QueryStringBindable[String]): QueryStringBindable[LocalDate] =
    new QueryStringBindable[LocalDate] {
      override def bind(key: String, params: Map[String, Seq[String]])
          : Option[Either[String, LocalDate]] = {
        for {
          dateStr <- strBinder.bind(key, params)
        } yield {
          dateStr match {
            case Right(dateStr) =>
              val fmt = DateTimeFormat.forPattern(localDatePattern)
              Try(fmt.parseDateTime(dateStr)) match {
                case Success(result) => Right(result.toLocalDate)
                case Failure(e) =>
                  Left(
                    "Cannot parse parameter " + key + " as small LocalDate: " + e.getMessage)
              }
            case _ => Left("Unable to bind LocalDate")
          }
        }
      }

      override def unbind(key: String, value: LocalDate): String = {
        val fmt = DateTimeFormat.forPattern(localDatePattern)
        strBinder.unbind(key, fmt.print(value))
      }

    }

  implicit def granularityQueryStringBinder(implicit
      strBinder: QueryStringBindable[String])
      : QueryStringBindable[Granularity] =
    new QueryStringBindable[Granularity] {
      override def bind(key: String, params: Map[String, Seq[String]])
          : Option[Either[String, Granularity]] = {
        strBinder.bind(key, params).map { bindResult =>
          for {
            str <- bindResult
            parseResult <- Json
              .fromJson[Granularity](JsString(str))
              .asEither
              .left
              .map(_ =>
                "Cannot parse parameter " + key + " as small Granularity")
          } yield parseResult
        }
      }

      override def unbind(key: String, value: Granularity): String = {
        strBinder.unbind(key, value.getClass.getSimpleName)
      }
    }

  implicit def bindableSeq[T: QueryStringBindable: ClassTag]
      : QueryStringBindable[Seq[T]] =
    new QueryStringBindable[Seq[T]] {
      def bind(
          key: String,
          params: Map[String, Seq[String]]): Option[Either[String, Seq[T]]] =
        Some(Right(bindSeq(key, params)))

      def unbind(key: String, values: Seq[T]): String = unbindSeq(key, values)

      private def bindSeq(key: String,
                          params: Map[String, Seq[String]]): Seq[T] = {
        for {
          values     <- params.get(key).toList
          rawValue   <- values
          splitValue <- rawValue.split(",")
          bound <- implicitly[QueryStringBindable[T]]
            .bind(key, Map(key -> Seq(splitValue)))
          value <- bound.toOption
        } yield value
      }

      private def unbindSeq(key: String, values: Iterable[T]): String = {
        (for (value <- values) yield {
          implicitly[QueryStringBindable[T]].unbind(key, value)
        }).mkString("&")
      }
    }

  implicit def UserIdPathBindable(implicit
      stringBinder: PathBindable[String]): PathBindable[UserId] =
    uuidBaseIdPathBindable[models.UserId](stringBinder, UserId.apply)

  implicit def ProjectIdPathBindable(implicit
      stringBinder: PathBindable[String]): PathBindable[ProjectId] =
    uuidBaseIdPathBindable[ProjectId](stringBinder, ProjectId.apply)

  implicit def ProjectIdIdStringBaseIdQueryStringBinder(implicit
      stringBinder: QueryStringBindable[String])
      : QueryStringBindable[ProjectId] =
    uuidBaseIdQueryStringBinder[ProjectId](stringBinder, ProjectId.apply)

  implicit def bsonObjectIdBaseIdQueryStringBinder(implicit
      stringBinder: PathBindable[String]): PathBindable[BookingId] =
    uuidBaseIdPathBindable[BookingId](stringBinder, BookingId.apply)

  implicit def BookingIdIdStringBaseIdQueryStringBinder(implicit
      stringBinder: QueryStringBindable[String])
      : QueryStringBindable[BookingId] =
    uuidBaseIdQueryStringBinder[BookingId](stringBinder, BookingId.apply)

  implicit def TagIdPathBindable(implicit
      stringBinder: PathBindable[String]): PathBindable[TagId] =
    stringBaseIdPathBindable[TagId](stringBinder, TagId.apply)

  implicit def TagIdStringBaseIdQueryStringBinder(implicit
      stringBinder: QueryStringBindable[String]): QueryStringBindable[TagId] =
    stringBaseIdQueryStringBinder[TagId](stringBinder, TagId.apply)

  implicit def OrganisationIdPathBindable(implicit
      stringBinder: PathBindable[String]): PathBindable[OrganisationId] =
    uuidBaseIdPathBindable[OrganisationId](stringBinder, OrganisationId.apply)

  implicit def InvitationIdPathBindable(implicit
      stringBinder: PathBindable[String]): PathBindable[InvitationId] =
    uuidBaseIdPathBindable[InvitationId](stringBinder, InvitationId.apply)

  implicit def PublicHolidayIdPathBindable(implicit
      stringBinder: PathBindable[String]): PathBindable[PublicHolidayId] =
    uuidBaseIdPathBindable[PublicHolidayId](stringBinder, PublicHolidayId.apply)
}
