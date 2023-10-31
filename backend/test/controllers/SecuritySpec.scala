/*
 *
 * Lasius - Open source time tracker for teams
 * Copyright (c) Tegonal Genossenschaft (https://tegonal.com)
 *
 * This file is part of Lasius.
 *
 * Lasius is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * Lasius is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Lasius. If not, see <https://www.gnu.org/licenses/>.
 */

package controllers

import core.{
  DBSession,
  DBSupport,
  MockCacheAware,
  TestApplication,
  TestDBSupport
}
import models._
import mongo.EmbedMongo
import org.apache.http.HttpStatus
import org.specs2.mock.Mockito
import play.api.libs.json.Json
import play.api.mvc._
import play.api.test._
import play.modules.reactivemongo.ReactiveMongoApi

import scala.concurrent._
import scala.concurrent.duration._
import scala.language.postfixOps

class SecuritySpec
    extends PlaySpecification
    with Results
    with Mockito
    with MockCacheAware
    with BodyParserUtils
    with TestApplication
    with EmbedMongo {
  sequential =>

  "HasToken" should {

    def runHasToken(controller: SecurityMock, request: Request[Unit]) = {
      // execute
      val result: Future[Result] = controller
        .HasToken(ignore(()), withinTransaction = false) {
          _ => _ => implicit request =>
            Future.successful(Ok)
        }
        .apply(request)

      // return results
      Await.ready(result, 2 seconds)
    }

    "Fail if token is missing in cookie" in new WithTestApplication {
      // prepare
      val controller             = new SecurityMock(reactiveMongoApi)
      val request: Request[Unit] = FakeRequest().asInstanceOf[Request[Unit]]

      // execute
      val result = runHasToken(controller, request)

      // check results
      status(result) === HttpStatus.SC_UNAUTHORIZED
      contentAsJson(result) === Json.obj(
        "message" -> "Invalid XSRF Token cookie")
    }

    "Fail if token is missing in request header" in new WithTestApplication {
      // prepare
      val controller = new SecurityMock(reactiveMongoApi)
      val token      = "ghvhvh"
      val request: Request[Unit] = FakeRequest()
        .withCookies(Cookie(controller.AuthTokenCookieKey, token))
        .asInstanceOf[Request[Unit]]

      // execute
      val result = runHasToken(controller, request)

      // check results
      status(result) === HttpStatus.SC_UNAUTHORIZED
      contentAsJson(result) === Json.obj("message" -> "No Token")
    }

    "Fail if token is missing in cache" in new WithTestApplication {
      // prepare
      val controller = new SecurityMock(reactiveMongoApi)
      val token      = "ghvhvh"
      val request = FakeRequest()
        .withCookies(Cookie(controller.AuthTokenCookieKey, token))
        .withHeaders((controller.AuthTokenHeader, token))
        .asInstanceOf[Request[Unit]]

      // execute
      val result = runHasToken(controller, request)

      // check results
      status(result) === HttpStatus.SC_UNAUTHORIZED
      contentAsJson(result) === Json.obj("message" -> "No Token")
    }

    "Fail if token in cookie and request header mismatches" in new WithApplication {
      // prepare
      val controller = new SecurityMock(reactiveMongoApi)
      val token      = "ghvhvh"
      val token2     = "kljnkln880"
      val request = FakeRequest()
        .withCookies(Cookie(controller.AuthTokenCookieKey, token))
        .withHeaders((controller.AuthTokenHeader, token2))
        .asInstanceOf[Request[Unit]]
      authTokenCache.set(token2, EntityReference(UserId(), "userId"))

      // execute
      val result = runHasToken(controller, request)

      // check results
      status(result) === HttpStatus.SC_UNAUTHORIZED
      contentAsJson(result) === Json.obj("message" -> "Invalid Token")
    }

    "Succeed in all cases" in new WithTestApplication {
      // prepare
      val controller = spy(new SecurityMock(reactiveMongoApi))
      val token      = "ghvhvh"
      val request = FakeRequest()
        .withCookies(Cookie(controller.AuthTokenCookieKey, token))
        .withHeaders(controller.AuthTokenHeader -> token)
        .asInstanceOf[Request[Unit]]
      authTokenCache.set(token, EntityReference(UserId(), "userId"))
      implicit val req = FakeRequest()

      // execute
      val result = runHasToken(controller, request)

      // check results
      status(result) === HttpStatus.SC_OK
    }
  }

  "HasRole" should {
    def runHasRole(controller: HasRoleSecurityMock,
                   role: UserRole = Administrator) = {
      // prepare
      val request = FakeRequest().asInstanceOf[Request[Unit]]

      // execute
      val result: Future[Result] = controller
        .HasUserRole(role, controller.parse.empty, withinTransaction = false) {
          _ => subject => _ => implicit request =>
            Future.successful(Ok)
        }
        .apply(request)

      // return results & wait until future is complete for testing purposes
      Await.ready(result, 2 seconds)
    }

    "return unauthorized when user can't get resolved" in new WithTestApplication {
      // prepare
      val controller = new HasRoleSecurityMock(reactiveMongoApi)
      controller.authConfig
        .resolveUser(any[EntityReference[UserId]])(any[ExecutionContext],
                                                   any[DBSession])
        .returns(Future.successful(None))

      // execute
      val result = runHasRole(controller)

      // check results
      there.was(
        one(controller.authConfig)
          .authorizationFailed(any[RequestHeader])(any[ExecutionContext]))
    }

    "return unauthorized when autorization failed" in new WithTestApplication {
      // prepare
      val controller = new HasRoleSecurityMock(reactiveMongoApi)
      controller.authConfig
        .authorizeUser(any[User], any[UserRole])(any[ExecutionContext])
        .returns(Future.successful(false))
      controller.authConfig
        .resolveUser(any[EntityReference[UserId]])(any[ExecutionContext],
                                                   any[DBSession])
        .returns(Future.successful(Some(UserMock.mock(FreeUser))))

      // execute
      val result = runHasRole(controller)

      // check results
      there.was(
        one(controller.authConfig)
          .authorizationFailed(any[RequestHeader])(any[ExecutionContext]))
    }

    "return InternalServerError on any failure" in new WithTestApplication {
      // prepare
      val controller = new HasRoleSecurityMock(reactiveMongoApi)
      controller.authConfig
        .authorizeUser(any[User], any[UserRole])(any[ExecutionContext])
        .returns(Future.failed(new RuntimeException))
      controller.authConfig
        .resolveUser(any[EntityReference[UserId]])(any[ExecutionContext],
                                                   any[DBSession])
        .returns(Future.successful(Some(UserMock.mock(FreeUser))))

      // execute
      val result = runHasRole(controller)

      // check results
      status(result) === HttpStatus.SC_INTERNAL_SERVER_ERROR
    }

    "Succeed if authorized" in new WithApplication {
      // prepare
      val controller = new HasRoleSecurityMock(reactiveMongoApi)
      controller.authConfig
        .authorizeUser(any[User], any[UserRole])(any[ExecutionContext])
        .returns(Future.successful(true))
      controller.authConfig
        .resolveUser(any[EntityReference[UserId]])(any[ExecutionContext],
                                                   any[DBSession])
        .returns(Future.successful(Some(UserMock.mock(FreeUser))))

      // execute
      val result = runHasRole(controller)

      // check results
      status(result) === HttpStatus.SC_OK
    }
  }
}

class SecurityMock(override val reactiveMongoApi: ReactiveMongoApi)
    extends AbstractController(Helpers.stubControllerComponents())
    with Security
    with SecurityComponentMock
    with MockCacheAware
    with TestDBSupport {}

object UserMock {
  def mock(role: UserRole): User =
    User(UserId(),
         "123",
         "email",
         "login",
         "firstname",
         "lastname",
         true,
         role,
         Seq(),
         settings = None)
}

class HasRoleSecurityMock(
    override val reactiveMongoApi: ReactiveMongoApi,
    subject: Subject = Subject("fsdfsdf", EntityReference(UserId(), "123")))
    extends AbstractController(Helpers.stubControllerComponents())
    with Security
    with SecurityComponentMock
    with MockCacheAware
    with TestDBSupport {

  override def HasToken[A](p: BodyParser[A], withinTransaction: Boolean)(
      f: DBSession => Subject => Request[A] => Future[Result])(implicit
      context: ExecutionContext): Action[A] = {
    Action.async(p) { implicit request =>
      withDBSession() { session =>
        f(session)(subject)(request)
      }
    }
  }
}
