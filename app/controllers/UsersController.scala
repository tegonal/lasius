package controllers

import play.api.libs.json._
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits._
import scala.util.{ Success, Failure }
import repositories.SecurityRepositoryComponent
import repositories.MongoSecurityRepositoryComponent

trait UsersController {
  // Cake pattern
  this: SecurityRepositoryComponent with Controller with Security =>

  /**
   * Retrieves a logged in user if the authentication token is valid.
   *
   * If the token is invalid, [[HasToken]] does not invoke this function.
   *
   * returns The user in JSON format.
   */
  def authUser() = HasToken(parse.empty) { subject =>
    implicit request => {
      userRepository.findById(subject.userId).map(_.map {
        case (user) =>
          Ok(Json.toJson(user))
      }.getOrElse(BadRequest))
    }
  }
}

object UsersController extends UsersController with Controller with Security with DefaultSecurityComponent with MongoSecurityRepositoryComponent
