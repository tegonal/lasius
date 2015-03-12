package models

import com.tegonal.play.json.TypedId._
import reactivemongo.bson.BSONObjectID
import play.api.libs.json._
import scala.util.Success
import org.joda.time.Duration
import org.joda.time.DateMidnight
import org.joda.time.DateTime
import models.BaseFormat.CompositeBaseId
import org.joda.time.LocalDate
import reactivemongo.bson.BSONDateTime
import reactivemongo.bson.BSONHandler
import org.joda.time.DateTimeFieldType

trait BaseEntity[I <: BaseId[_]] {
  val id: I
}

object BaseFormat {
  trait CompositeBaseId[I1, I2] extends BaseId[((String, I1), (String, I2))]

  trait BaseBSONObjectId extends BaseId[BSONObjectID]

  //extended format function
  def idformat[I <: BaseBSONObjectId](implicit fact: Factory[BSONObjectID, I]) = new BSONObjectIdTypedIdFormat[I]
  def idformat[I <: CompositeBaseId[I1, I2], I1, I2](implicit fact: (I1, I2) => I, f: Format[I1], ff2: Format[I2]) = new CompositeIdTypedIdFormat[I, I1, I2]

  implicit val durationFormat: Format[Duration] = new Format[Duration] {
    def reads(json: JsValue): JsResult[Duration] = json match {

      case JsNumber(millis) => {
        JsSuccess(Duration.millis(millis.toLong))
      }
      case _ => JsError(s"Unexpected JSON value $json")
    }

    def writes(duration: Duration): JsValue = JsNumber(duration.getMillis)
  }

  implicit object BSONDateTimeHandler extends BSONHandler[BSONDateTime, DateTime] {
    def read(time: BSONDateTime) = new DateTime(time.value)
    def write(jdtime: DateTime) = BSONDateTime(jdtime.getMillis)
  }
}

class BSONObjectIdTypedIdFormat[I <: BaseId[BSONObjectID]](implicit fact: Factory[BSONObjectID, I]) extends Format[I] {
  def reads(json: JsValue): JsResult[I] = json match {

    case JsString(value) => {
      BSONObjectID.parse(value) match {
        case Success(id) =>
          JsSuccess(fact(id))
        case _ =>
          JsError(s"Unexpected JSON value $json")
      }
    }
    case _ => JsError(s"Unexpected JSON value $json")
  }

  def writes(id: I): JsValue = JsString(id.value.stringify)
}

class CompositeIdTypedIdFormat[I <: CompositeBaseId[I1, I2], I1, I2](implicit fact: (I1, I2) => I, f1: Format[I1], f2: Format[I2]) extends Format[I] {
  def reads(json: JsValue): JsResult[I] = json match {
    case JsObject(values) => {
      Json.fromJson[I1](values(0)._2) match {
        case JsSuccess(i1, _) =>
          Json.fromJson[I2](values(1)._2) match {
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