package helpers

import models.Subject
import models.User
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import play.api.Logger
import repositories.UserRepository
import repositories.SecurityRepositoryComponent
import models.UserId

trait UserHelper {
  self: SecurityRepositoryComponent =>
  def withUser[R](errorResult: R)(f: User => Future[R])(implicit subject: Subject, context: ExecutionContext) = {
    forUser(subject.userId)(errorResult)(f)
  }

  def forUser[R](userId: UserId)(errorResult: R)(f: User => Future[R])(implicit context: ExecutionContext) = {
    userRepository.findById(userId) flatMap { o =>
      o.fold(Future.successful(errorResult)) { user =>
        f(user)
      }
    }
  }
}