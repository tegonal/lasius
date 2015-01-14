import play.api.mvc.PathBindable
import models._
import com.tegonal.play.json.TypedId._
import play.api.mvc.QueryStringBindable
import org.joda.time.DateTime
import java.text.SimpleDateFormat
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat

object `package` {

  implicit def OptionBindable[T: PathBindable] = new PathBindable[Option[T]] {
    def bind(key: String, value: String): Either[String, Option[T]] =
      implicitly[PathBindable[T]].
        bind(key, value).
        fold(
          left => Left(left),
          right => Right(Some(right)))

    def unbind(key: String, value: Option[T]): String = value map (_.toString) getOrElse ""
  }

  implicit def StringBaseIdPathBindable[T <: StringBaseId](implicit stringBinder: PathBindable[String], fact: String => T) = new PathBindable[T] {

    def bind(key: String, value: String): Either[String, T] =
      for {
        id <- stringBinder.bind(key, value).right
      } yield fact(id)

    def unbind(key: String, id: T): String =
      stringBinder.unbind(key, id.value)
  }

  implicit def NumberBaseIdPathBindable[T <: NumberBaseId](implicit numberBinder: PathBindable[Number], fact: Number => T) = new PathBindable[T] {

    def bind(key: String, value: String): Either[String, T] =
      for {
        id <- numberBinder.bind(key, value).right
      } yield fact(id)

    def unbind(key: String, id: T): String =
      numberBinder.unbind(key, id.value)
  }

  implicit def StringBaseIdQueryStringBinder[T <: StringBaseId](implicit strBinder: QueryStringBindable[String], fact: String => T) = new QueryStringBindable[T] {
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

  implicit def NumberBaseIdQueryStringBinder[T <: NumberBaseId](implicit strBinder: QueryStringBindable[Number], fact: Number => T) = new QueryStringBindable[T] {
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

  implicit def DateTimeQueryStringBinder(implicit strBinder: QueryStringBindable[String]) = new QueryStringBindable[DateTime] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, DateTime]] = {
      for {
        dateStr <- strBinder.bind(key, params)
      } yield {
        dateStr match {
          case (Right(dateStr)) => {
            val formatter = new SimpleDateFormat(
              "EEE, d MMM yyyy HH:mm:ss z");
            try {
              val someDate = formatter.parse(dateStr);
              Right(new DateTime(someDate.getTime(), DateTimeZone.UTC))
            } catch {
              case e: NumberFormatException => Left("Cannot parse parameter " + key + " as DateTime: " + e.getMessage)
            }
          }
          case _ => Left("Unable to bind DateTime")
        }
      }
    }

    override def unbind(key: String, value: DateTime) = {
      val fmt = DateTimeFormat
        .forPattern("EEE, d MMM yyyy HH:mm:ss z");
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
        bound <- implicitly[QueryStringBindable[T]].bind(key, Map(key -> Seq(rawValue)))
        value <- bound.right.toOption
      } yield value
    }

    private def unbindSeq[T: QueryStringBindable](key: String, values: Iterable[T]): String = {
      (for (value <- values) yield {
        implicitly[QueryStringBindable[T]].unbind(key, value)
      }).mkString("&")
    }
  }

  implicit def UserIdPathBindable(implicit stringBinder: PathBindable[String]) = StringBaseIdPathBindable[UserId](stringBinder, UserId.apply _)
  implicit def ProjectIdPathBindable(implicit stringBinder: PathBindable[String]) = StringBaseIdPathBindable[ProjectId](stringBinder, ProjectId.apply _)
  implicit def TagIdPathBindable(implicit stringBinder: PathBindable[String]) = StringBaseIdPathBindable[TagId](stringBinder, TagId.apply _)
  implicit def TagIdStringBaseIdQueryStringBinder(implicit stringBinder: QueryStringBindable[String]) = StringBaseIdQueryStringBinder[TagId](stringBinder, TagId.apply _)
}