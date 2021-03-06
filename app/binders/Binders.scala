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
package binders

import org.joda.time.format._
import org.joda.time._
import play.api.mvc._
import com.tegonal.play.json.TypedId._
import java.text.SimpleDateFormat
import models._
import java.text.ParseException
import models.BaseFormat.BaseBSONObjectId
import reactivemongo.bson.BSONObjectID
import scala.util._

object Binders {

  val formatShort = "ddMMyyyyHHmm"
  val format = "ddMMyyyyHHmmss"

  implicit def OptionBindable[T: PathBindable] = new PathBindable[Option[T]] {
    override def bind(key: String, value: String): Either[String, Option[T]] =
      implicitly[PathBindable[T]].
        bind(key, value).
        fold(
          left => Left(left),
          right => Right(Some(right)))

    override def unbind(key: String, value: Option[T]): String = value map (_.toString) getOrElse ""
  }

  implicit def stringBaseIdPathBindable[T <: StringBaseId](implicit stringBinder: PathBindable[String], fact: String => T) = new PathBindable[T] {

    override def bind(key: String, value: String): Either[String, T] =
      for {
        id <- stringBinder.bind(key, value).right
      } yield fact(id)

    override def unbind(key: String, id: T): String =
      stringBinder.unbind(key, id.value)
  }
  
  implicit def bsonObjectIdBaseIdPathBindable[T <: BaseBSONObjectId](implicit stringBinder: PathBindable[String], fact: BSONObjectID => T) = new PathBindable[T] {

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

  implicit def numberBaseIdPathBindable[T <: NumberBaseId](implicit numberBinder: PathBindable[Number], fact: Number => T) = new PathBindable[T] {

    override def bind(key: String, value: String): Either[String, T] =
      for {
        id <- numberBinder.bind(key, value).right
      } yield fact(id)

    override def unbind(key: String, id: T): String =
      numberBinder.unbind(key, id.value)
  }

  implicit def stringBaseIdQueryStringBinder[T <: StringBaseId](implicit strBinder: QueryStringBindable[String], fact: String => T) = new QueryStringBindable[T] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, T]] = {
      for {
        str <- strBinder.bind(key, params)
      } yield {
        str match {
          case (Right(id)) => {
            Right(fact(id))
          }
          case _ => Left("Unable to bind id")
        }
      }
    }

    override def unbind(key: String, id: T) = {
      strBinder.unbind(key, id.value)
    }
  }
  
  implicit def bsonObjectIdBaseIdQueryStringBinder[T <: BaseBSONObjectId](implicit strBinder: QueryStringBindable[String], fact: BSONObjectID => T) = new QueryStringBindable[T] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, T]] = {
    for {
        str <- strBinder.bind(key, params)
      } yield {
      str match {case (Right(id)) =>     
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

    override def unbind(key: String, id: T) = {
      strBinder.unbind(key, id.value.toString)
    }
  }

  implicit def numberBaseIdQueryStringBinder[T <: NumberBaseId](implicit strBinder: QueryStringBindable[Number], fact: Number => T) = new QueryStringBindable[T] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, T]] = {
      for {
        str <- strBinder.bind(key, params)
      } yield {
        str match {
          case (Right(id)) => {
            Right(fact(id))
          }
          case _ => Left("Unable to bind id")
        }
      }
    }

    override def unbind(key: String, id: T) = {
      strBinder.unbind(key, id.value)
    }
  }

  implicit def dateTimeQueryStringBinder(implicit strBinder: QueryStringBindable[String]) = new QueryStringBindable[DateTime] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, DateTime]] = {
      for {
        dateStr <- strBinder.bind(key, params)
      } yield {
        dateStr match {
          case (Right(dateStr)) => {
            val formatter = new SimpleDateFormat(format);
            try {
              val someDate = formatter.parse(dateStr);
              Right(new DateTime(someDate.getTime(), DateTimeZone.getDefault()))
            } catch {
              case e: ParseException =>
                val formatterShort = new SimpleDateFormat(formatShort);
                try {
                  val someDate = formatterShort.parse(dateStr);
                  Right(new DateTime(someDate.getTime(), DateTimeZone.getDefault()))
                } catch {
                  case e: ParseException =>
                    Left("Cannot parse parameter " + key + " as small DateTime: " + e.getMessage)
                }
            }
          }
          case _ => Left("Unable to bind DateTime")
        }
      }
    }

    override def unbind(key: String, value: DateTime) = {
      val fmt = DateTimeFormat.forPattern(format);
      strBinder.unbind(key, fmt.print(value))
    }

  }

  implicit def bindableSeq[T: QueryStringBindable] = new QueryStringBindable[Seq[T]] {
    def bind(key: String, params: Map[String, Seq[String]]) = Some(Right(bindSeq[T](key, params)))
    def unbind(key: String, values: Seq[T]) = unbindSeq(key, values)

    private def bindSeq[T: QueryStringBindable](key: String, params: Map[String, Seq[String]]): Seq[T] = {
      for {
        values <- params.get(key).toList
        rawValue <- values
        splitValue <- rawValue.split(",")
        bound <- implicitly[QueryStringBindable[T]].bind(key, Map(key -> Seq(splitValue)))
        value <- bound.right.toOption
      } yield value
    }

    private def unbindSeq[T: QueryStringBindable](key: String, values: Iterable[T]): String = {
      (for (value <- values) yield {
        implicitly[QueryStringBindable[T]].unbind(key, value)
      }).mkString("&")
    }
  }

  implicit def UserIdPathBindable(implicit stringBinder: PathBindable[String]) = stringBaseIdPathBindable[models.UserId](stringBinder, UserId.apply _)
  implicit def ProjectIdPathBindable(implicit stringBinder: PathBindable[String]) = stringBaseIdPathBindable[ProjectId](stringBinder, ProjectId.apply _)
  implicit def ProjectIdIdStringBaseIdQueryStringBinder(implicit stringBinder: QueryStringBindable[String]) = stringBaseIdQueryStringBinder[ProjectId](stringBinder, ProjectId.apply _)
  implicit def CategoryIdPathBindable(implicit stringBinder: PathBindable[String]) = stringBaseIdPathBindable[CategoryId](stringBinder, CategoryId.apply _)
  implicit def CategoryIdIdStringBaseIdQueryStringBinder(implicit stringBinder: QueryStringBindable[String]) = stringBaseIdQueryStringBinder[CategoryId](stringBinder, CategoryId.apply _)
  implicit def BookingIdPathBindable(implicit stringBinder: PathBindable[String]) = stringBaseIdPathBindable[BookingId](stringBinder, BookingId.apply _)
  implicit def BookingIdIdStringBaseIdQueryStringBinder(implicit stringBinder: QueryStringBindable[String]) = stringBaseIdQueryStringBinder[BookingId](stringBinder, BookingId.apply _)
  implicit def TagIdPathBindable(implicit stringBinder: PathBindable[String]) = stringBaseIdPathBindable[TagId](stringBinder, TagId.apply _)
  implicit def TagIdStringBaseIdQueryStringBinder(implicit stringBinder: QueryStringBindable[String]) = stringBaseIdQueryStringBinder[TagId](stringBinder, TagId.apply _)
  implicit def TeamIdPathBindable(implicit stringBinder: PathBindable[String]) = bsonObjectIdBaseIdPathBindable[TeamId](stringBinder, TeamId.apply _)
  implicit def TeamIdBaseIdQueryStringBinder(implicit stringBinder: QueryStringBindable[String]) = bsonObjectIdBaseIdQueryStringBinder[TeamId](stringBinder, TeamId.apply _)
}