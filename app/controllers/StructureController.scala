package controllers

import play.api.mvc.Controller

import repositories.StructureRepository
import repositories.BasicRepositoryComponent
import play.api.mvc.Action
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

class StructureController {
  self: Controller with BasicRepositoryComponent =>

  def getCategories() = Action.async {
    structureRepository.findAllCategories map { categories =>
      Ok(Json.toJson(categories))
    }
  }
}