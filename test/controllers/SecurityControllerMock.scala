package controllers

import scala.concurrent.ExecutionContext

import scala.concurrent.Future
import models._
import play.api.mvc.Action
import play.api.mvc.BodyParser
import play.api.mvc.Controller
import play.api.mvc.Request
import play.api.mvc.RequestHeader
import play.api.mvc.Result
import reactivemongo.bson.BSONObjectID
import play.api.Logger

class SecurityControllerMock(token: String = "", userId: UserId = UserId("someUserId"), authorized: Future[Boolean] = Future.successful(true), user: Option[User] = None, authorizationFailedResult: Result = null) extends SecurityComponentMock with Controller with Security {
  override def HasToken[A](p: BodyParser[A] = parse.anyContent)(
    f: Subject => Request[A] => Future[Result])(implicit context: ExecutionContext): Action[A] = {
    Action.async(p) { implicit request =>
      f(Subject(token, userId))(request)
    }
  }

  override def HasRole[A, R <: Role](role: R, p: BodyParser[A] = parse.anyContent)(
    f: Subject => Request[A] => Future[Result])(implicit context: ExecutionContext): Action[A] = {
    Action.async(p) { implicit request =>
      f(Subject(token, userId))(request)
    }
  }
}