package controllers

import play.api.mvc.Controller
import repositories.StructureRepository
import repositories.BasicRepositoryComponent
import play.api.mvc.Action
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import models.UserId
import repositories.MongoBasicRepositoryComponent
import models.Category
import models.Project
import models.CategoryId

class StructureController {
  self: Controller with BasicRepositoryComponent =>

  case class ProjectContainer(project: Project, categoryId: CategoryId, name: String)

  object ProjectContainer {
    implicit val projContFormat: Format[ProjectContainer] = Json.format[ProjectContainer]
  }

  def getCategories(userId: UserId) = Action.async {
    structureRepository.findAllCategories map { categories =>
      //invert relationship from category to project
      val projects = for {
        cat <- categories
        proj <- cat.projects
      } yield {
        //remove reference to projects
        ProjectContainer(proj, cat.id, s"${proj.id.value}@${cat.id.value}")
      }
      Ok(Json.toJson(projects))
    }
  }
}

object StructureController extends StructureController with MongoBasicRepositoryComponent with Controller