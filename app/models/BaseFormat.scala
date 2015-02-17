package models

import com.tegonal.play.json.TypedId._
import reactivemongo.bson.BSONObjectID
import play.api.libs.json._
import scala.util.Success
import org.joda.time.Duration
import org.joda.time.DateMidnight
import org.joda.time.DateTime

trait BaseEntity[ID <: BaseId[_]] {
  val id: ID
}

object BaseFormat {
  trait BaseBSONObjectId extends BaseId[BSONObjectID]

  //extended format function
  def idformat[I <: BaseBSONObjectId](implicit fact: Factory[BSONObjectID, I]) = new BSONObjectIdTypedIdFormat[I]

  implicit val durationFormat: Format[Duration] = new Format[Duration] {
    def reads(json: JsValue): JsResult[Duration] = json match {

      case JsNumber(millis) => {
        JsSuccess(Duration.millis(millis.toLong))
      }
      case _ => JsError(s"Unexpected JSON value $json")
    }

    def writes(duration: Duration): JsValue = JsNumber(duration.getMillis)
  }

  implicit val dateMidnightFormat: Format[DateMidnight] = new Format[DateMidnight] {
    def reads(json: JsValue): JsResult[DateMidnight] = json match {

      case JsNumber(millis) => {
        JsSuccess(new DateTime(millis).toDateMidnight())
      }
      case _ => JsError(s"Unexpected JSON value $json")
    }

    def writes(duration: DateMidnight): JsNumber = JsNumber(duration.getMillis)
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