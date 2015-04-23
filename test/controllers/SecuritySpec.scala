package controllers

import scala.concurrent.Future
import play.api.mvc._
import play.api.test._
import play.api.test.Helpers._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.mutable._
import play.api.libs.json.Json
import org.mockito.Mockito._
import org.mockito.Matchers._
import models._
import reactivemongo.bson.BSONObjectID
import views.html.defaultpages.badRequest
import play.api.Logger
import org.apache.http.HttpStatus
import controllers.ApplicationController._
import play.cache.Cache
import scala.concurrent.ExecutionContext
import org.specs2.mock.Mockito
import org.mockito.internal.stubbing.answers.DoesNothing
import scala.concurrent._
import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
class SecuritySpec extends PlaySpecification with Results with Mockito {
  sequential =>
  "HasToken" should {
    "Fail if token is missing in cookie" in new WithApplication() {
      //prepare    
      val controller = new SecurityMockImpl()
      val request = FakeRequest().asInstanceOf[Request[Unit]]

      //execute
      val result = runHasToken(controller, request)

      //check results
      status(result) === HttpStatus.SC_UNAUTHORIZED
      contentAsJson(result) === Json.obj("message" -> "Invalid XSRF Token cookie")
    }

    "Fail if token is missing in request header" in new WithApplication() {
      //prepare    
      val controller = new SecurityMockImpl()
      val token = "ghvhvh"
      val request = FakeRequest().withCookies(Cookie(AuthTokenCookieKey, token)).asInstanceOf[Request[Unit]]

      //execute
      val result = runHasToken(controller, request)

      //check results
      status(result) === HttpStatus.SC_UNAUTHORIZED
      contentAsJson(result) === Json.obj("message" -> "No Token")
    }

    "Fail if token is missing in cache" in new WithApplication() {
      //prepare    
      val controller = new SecurityMockImpl()
      val token = "ghvhvh"
      val request = FakeRequest().withCookies(Cookie(AuthTokenCookieKey, token)).withHeaders((AuthTokenHeader, token)).asInstanceOf[Request[Unit]]

      //execute
      val result = runHasToken(controller, request)

      //check results
      status(result) === HttpStatus.SC_UNAUTHORIZED
      contentAsJson(result) === Json.obj("message" -> "No Token")
    }

    "Fail if token in cookie and request header mismatches" in new WithApplication() {
      //prepare    
      val controller = new SecurityMockImpl()
      val token = "ghvhvh"
      val token2 = "kljnkln880"
      val request = FakeRequest().withCookies(Cookie(AuthTokenCookieKey, token)).withHeaders((AuthTokenHeader, token2)).asInstanceOf[Request[Unit]]
      Cache.set(token2, UserId("userId"))

      //execute
      val result = runHasToken(controller, request)

      //check results
      status(result) === HttpStatus.SC_UNAUTHORIZED
      contentAsJson(result) === Json.obj("message" -> "Invalid Token")
    }

    "Succeed in all cases" in new WithApplication() {
      //prepare    
      val controller = spy(new SecurityMockImpl())
      val token = "ghvhvh"
      val request = FakeRequest().withCookies(Cookie(AuthTokenCookieKey, token)).withHeaders(AuthTokenHeader -> token).asInstanceOf[Request[Unit]]
      Cache.set(token, UserId("userId"))
      implicit val req = FakeRequest()

      //execute
      val result = runHasToken(controller, request)

      //check results
      status(result) === HttpStatus.SC_OK
    }

    def runHasToken(controller: SecurityMock, request: Request[Unit]) = {
      //execute
      val result: Future[Result] = controller.HasToken(parse.empty) { subject =>
        implicit request =>
          Future.successful(Ok)
      }.apply(request)

      //return results
      Await.ready(result, 2 seconds)
    }
  }

  "HasRole" should {
    def runHasRole(controller: HasRoleSecurityMock, role: Role = Administrator) = {
      //prepare    
      val request = FakeRequest().asInstanceOf[Request[Unit]]

      //execute
      val result: Future[Result] = controller.HasRole(role, parse.empty) { subject =>
        implicit request =>
          Future.successful(Ok)
      }.apply(request)

      //return results & wait until future is complete for testing purposes
      Await.ready(result, 2 seconds)
    }

    "return unauthorized when user can't get resolved" in new WithApplication() {
      //prepare    
      val controller = new HasRoleSecurityMock()
      controller.authConfig.resolveUser(any[UserId])(any[ExecutionContext]) returns Future.successful(None)

      //execute
      val result = runHasRole(controller)

      //check results
      there was one(controller.authConfig).authorizationFailed(any[RequestHeader])(any[ExecutionContext])
    }

    "return unauthorized when autorization failed" in new WithApplication() {
      //prepare    
      val controller = new HasRoleSecurityMock()
      controller.authConfig.authorize(any[User], any[Role])(any[ExecutionContext]) returns Future.successful(false)
      controller.authConfig.resolveUser(any[UserId])(any[ExecutionContext]) returns Future.successful(Some(UserMock.mock(FreeUser)))

      //execute
      val result = runHasRole(controller)

      //check results
      there was one(controller.authConfig).authorizationFailed(any[RequestHeader])(any[ExecutionContext])
    }

    "return InternalServerError on any failure" in new WithApplication() {
      //prepare    
      val controller = new HasRoleSecurityMock()
      controller.authConfig.authorize(any[User], any[Role])(any[ExecutionContext]) returns Future.failed(new RuntimeException)
      controller.authConfig.resolveUser(any[UserId])(any[ExecutionContext]) returns Future.successful(Some(UserMock.mock(FreeUser)))

      //execute
      val result = runHasRole(controller)

      //check results
      status(result) === HttpStatus.SC_INTERNAL_SERVER_ERROR
    }

    "Succeed if authorized" in new WithApplication() {
      //prepare    
      val controller = new HasRoleSecurityMock()
      controller.authConfig.authorize(any[User], any[Role])(any[ExecutionContext]) returns Future.successful(true)
      controller.authConfig.resolveUser(any[UserId])(any[ExecutionContext]) returns Future.successful(Some(UserMock.mock(FreeUser)))

      //execute
      val result = runHasRole(controller)

      //check results
      status(result) === HttpStatus.SC_OK
    }
  }
}
trait SecurityMock extends Security with SecurityComponentMock with Controller {
}

class SecurityMockImpl extends SecurityMock

object UserMock {
  def mock(role: Role): User = User(UserId("123"),
    "email",
    "login",
    "firstname",
    "lastname",
    true,
    role,
    Seq(),
    Seq())
}

class HasRoleSecurityMock(subject: Subject = Subject("fsdfsdf", UserId("123"))) extends Security with SecurityComponentMock with Controller {
  override def HasToken[A](p: BodyParser[A] = parse.anyContent)(
    f: Subject => Request[A] => Future[Result])(implicit context: ExecutionContext): Action[A] = {
    Action.async(p) { implicit request =>
      f(subject)(request)
    }
  }
}
