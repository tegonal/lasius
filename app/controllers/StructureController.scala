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

import actors.TagCache.{CachedTags, GetTags}
import akka.pattern.ask
import akka.util.Timeout
import core.{DefaultCacheAware, DefaultSystemServicesAware, SystemServicesAware}
import helpers.UserHelper
import models._
import play.api.libs.json._
import play.api.mvc.Controller
import repositories._

import scala.concurrent.Future
import scala.concurrent.duration._

class StructureController extends UserHelper {
  self: Controller with BasicRepositoryComponent with Security with SystemServicesAware =>

  case class ProjectContainer(project: Project, categoryId: CategoryId, name: String, tagCache:Seq[BaseTag])

  object ProjectContainer {
    implicit val projContFormat: Format[ProjectContainer] = Json.format[ProjectContainer]
  }

  def getCategories() = HasRole(FreeUser, parse.empty) {    
    implicit subject =>
      implicit request =>
        withUser(BadRequest("No user found for login")) { user =>
          //invert relationship from category to project
          Future.sequence(for {
            cat <- user.categories
            proj <- cat.projects            
          } yield {
            for {
              tags <- getTags(proj.id)
            } yield {            
              //remove reference to projects
              ProjectContainer(proj, cat.id, s"${proj.id.value}@${cat.id.value}", (tags ++ proj.tags).toSeq.sortBy(_.id.value))
            }
          }) map(p => Ok(Json.toJson(p)))
        }
  }
  
  def getTags(projectId: ProjectId): Future[Set[BaseTag]] = {
        implicit val timeout = Timeout(5 seconds) // needed for `?` below
        val future = systemServices.tagCache ? GetTags(projectId)
        future.map{ result =>
          val tagResult = result.asInstanceOf[CachedTags]
          
          tagResult.tags
        }
  }
}

object StructureController extends StructureController with MongoBasicRepositoryComponent with Controller with Security with DefaultSecurityComponent with DefaultCacheAware with DefaultSystemServicesAware