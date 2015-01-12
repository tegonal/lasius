package models

import com.tegonal.play.json.TypedId._
import reactivemongo.bson.BSONObjectID
import play.api.libs.json._
import scala.util.Success

trait BaseEntity[ID <: BaseId[_]] {
  val id: ID
}

object BaseFormat {
  trait BaseBSONObjectId extends BaseId[BSONObjectID]

  //extended format function
  def idformat[I <: BaseBSONObjectId](implicit fact: Factory[BSONObjectID, I]) = new BSONObjectIdTypedIdFormat[I]
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