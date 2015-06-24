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
package controllers

import play.api.mvc.Controller

import repositories._
import play.api.mvc.Action
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import models._
import helpers.UserHelper
import scala.concurrent.Future

class StructureController extends UserHelper {
  self: Controller with BasicRepositoryComponent with Security =>

  case class ProjectContainer(project: Project, categoryId: CategoryId, name: String)

  object ProjectContainer {
    implicit val projContFormat: Format[ProjectContainer] = Json.format[ProjectContainer]
  }

  def getCategories() = HasRole(FreeUser, parse.empty) {
    implicit subject =>
      implicit request =>
        withUser(BadRequest("No user found for login")) { user =>
          //invert relationship from category to project
          val projects = for {
            cat <- user.categories
            proj <- cat.projects
          } yield {
            //remove reference to projects
            ProjectContainer(proj, cat.id, s"${proj.id.value}@${cat.id.value}")
          }
          Future.successful(Ok(Json.toJson(projects)))
        }
  }
}

object StructureController extends StructureController with MongoBasicRepositoryComponent with Controller with Security with DefaultSecurityComponent