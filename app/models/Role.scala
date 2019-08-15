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

import play.api.libs.json._

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
