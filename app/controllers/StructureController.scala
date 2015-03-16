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