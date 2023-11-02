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

import core.{DBSession, MockCacheAware, TestApplication, TestDBSupport}
import models._
import mongo.EmbedMongo
import org.apache.http.HttpStatus
import org.pac4j.core.profile.CommonProfile
import org.pac4j.play.scala.{
  AuthenticatedRequest,
  Security => Pac4jSecurity,
  SecurityComponents
}
import org.specs2.mock.Mockito
import play.api.mvc._
import play.api.test._
import play.modules.reactivemongo.ReactiveMongoApi
import util.SecurityComponents

import javax.inject.Inject
import scala.concurrent._
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.reflect.ClassTag

class SecuritySpec
    extends PlaySpecification
    with Results
    with Mockito
    with MockCacheAware
    with BodyParserUtils
    with TestApplication
    with EmbedMongo {
  sequential =>

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

class SecurityMock[P <: CommonProfile](
    @Inject
    override val reactiveMongoApi: ReactiveMongoApi,
    override val controllerComponents: SecurityComponents)
    extends BaseController
    with Security[P]
    with SecurityComponentMock
    with MockCacheAware
    with TestDBSupport
    with Pac4jSecurity[P] {}

object UserMock {
  def mock(role: UserRole): User =
    User(id = UserId(),
         key = "123",
         email = "email",
         firstName = "firstname",
         lastName = "lastname",
         active = true,
         role = role,
         organisations = Seq(),
         settings = None)
}

class HasRoleSecurityMock(
    override val reactiveMongoApi: ReactiveMongoApi,
    override val controllerComponents: SecurityComponents =
      SecurityComponents.stubSecurityComponents(),
    private val subject: Subject[CommonProfile] =
      Subject(new CommonProfile(), EntityReference(UserId(), "123")))
    extends BaseController
    with Security[CommonProfile]
    with SecurityComponentMock
    with MockCacheAware
    with TestDBSupport
    with Pac4jSecurity[CommonProfile] {

  override def HasToken[A](p: BodyParser[A],
                           withinTransaction: Boolean,
                           clients: String)(
      f: DBSession => Subject[CommonProfile] => AuthenticatedRequest[
        A] => Future[Result])(implicit
      context: ExecutionContext,
      ct: ClassTag[CommonProfile]): Action[A] =
    Action.async(p) { implicit request =>
      withDBSession() { session =>
        f(session)(subject)(
          AuthenticatedRequest(List(subject.profile), request))
      }
    }
}
