package models

import play.api.libs.json._
import play.api.Logger

sealed trait Role
case object FreeUser extends Role
case object Administrator extends Role

object Role {
  //TODO: replace by a macro
  implicit val roleFmt: Format[Role] = new Format[Role] {
    def reads(json: JsValue): JsResult[Role] = json match {
      case JsString("FreeUser") => JsSuccess(FreeUser)
      case JsString("Administrator") => JsSuccess(Administrator)
      case _ => JsError(s"Unexpected JSON value $json")
    }

    def writes(role: Role): JsValue = {
      role match {
        case FreeUser => JsString(FreeUser.toString)
        case Administrator => JsString(Administrator.toString)
        case _ => JsNull
      }
    }
  }
}
