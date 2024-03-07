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

import core._
import models._
import mongo.EmbedMongo
import org.specs2.mock.Mockito
import org.specs2.mock.mockito.MockitoMatchers
import play.api.mvc._
import play.api.test._
import play.modules.reactivemongo.ReactiveMongoApi
import repositories.{UserMongoRepository, UserRepository}
import util.{Awaitable, MockAwaitable}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class UsersControllerSpec
    extends PlaySpecification
    with Mockito
    with Results
    with MockitoMatchers
    with EmbedMongo
    with TestApplication {

  "update personal user data" should {
    "badrequest if no update field is specified" in new WithUsersControllerMock {
      val request: FakeRequest[PersonalDataUpdate] = FakeRequest()
        .withBody(
          PersonalDataUpdate(
            email = None,
            firstName = None,
            lastName = None
          ))
      val result: Future[Result] = controller.updatePersonalData()(request)

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo(
        "At least one field needs to be defined as update for user: someUserId")
    }

    "badrequest if tried to update with no email" in new WithUsersControllerMock {
      val request: FakeRequest[PersonalDataUpdate] = FakeRequest()
        .withBody(
          PersonalDataUpdate(
            email = Some("incorrectEmail"),
            firstName = None,
            lastName = None
          ))
      val result: Future[Result] = controller.updatePersonalData()(request)

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo(
        "Not a valid email address 'incorrectEmail'")
    }

    "badrequest email address user already exists" in new WithUsersControllerMock {
      // initialize
      val user2: User = User(UserId(),
                             "user2",
                             "my-email@test.com",
                             "no-hash",
                             "my",
                             "name",
                             active = true,
                             FreeUser,
                             Seq(),
                             settings = None)
      controller
        .withDBSession() { implicit dbSession =>
          controller.userRepository.upsert(user2)
        }
        .awaitResult()

      val request: FakeRequest[PersonalDataUpdate] = FakeRequest()
        .withBody(
          PersonalDataUpdate(
            email = Some(user2.email),
            firstName = Some("FirstName"),
            lastName = None
          ))
      val result: Future[Result] = controller.updatePersonalData()(request)

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo("Email Address already registered")
    }

    "update all fields correctly" in new WithUsersControllerMock {
      val request: FakeRequest[PersonalDataUpdate] = FakeRequest()
        .withBody(
          PersonalDataUpdate(
            email = Some("test@test.com"),
            firstName = Some("newFirstName"),
            lastName = Some("newLastName")
          ))
      val result: Future[Result] = controller.updatePersonalData()(request)

      status(result) must equalTo(OK)
      contentAsJson(result).as[UserDTO] must like {
        case UserDTO(_,
                     _,
                     "test@test.com",
                     "newFirstName",
                     "newLastName",
                     true,
                     _,
                     _,
                     _) =>
          1 === 1
      }
    }
  }

  "update other user data" should {
    "forbidden user does not exist" in new WithUsersControllerMock {
      val request: FakeRequest[PersonalDataUpdate] = FakeRequest()
        .withBody(
          PersonalDataUpdate(
            email = None,
            firstName = Some("FirstName"),
            lastName = None
          ))
      val teamId: OrganisationId = OrganisationId()
      val userId: UserId         = UserId()
      val result: Future[Result] =
        controller.updateUserData(teamId, userId)(request)

      status(result) must equalTo(FORBIDDEN)
    }
  }

  "change password" should {
    "badrequest if password does not match" in new WithUsersControllerMock {
      val request: FakeRequest[PasswordChangeRequest] = FakeRequest()
        .withBody(
          PasswordChangeRequest(
            password = "wrong-password",
            newPassword = "1d3dsaAad34212"
          ))
      val result: Future[Result] = controller.changePassword()(request)

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo("Provided password does not match")
    }

    "badrequest if new password does not match policy" in new WithUsersControllerMock {
      val request: FakeRequest[PasswordChangeRequest] = FakeRequest()
        .withBody(
          PasswordChangeRequest(
            password = controller.password,
            newPassword = "short-pwd"
          ))
      val result: Future[Result] = controller.changePassword()(request)

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo("password policy not satisfied")
    }

    "ok" in new WithUsersControllerMock {
      val newPassword = "1d3dsaAad34212"

      val request: FakeRequest[PasswordChangeRequest] = FakeRequest()
        .withBody(
          PasswordChangeRequest(
            password = controller.password,
            newPassword = newPassword
          ))
      val result: Future[Result] = controller.changePassword()(request)

      status(result) must equalTo(OK)

      // authenticate with changed password afterwards
      val loginResult: Option[User] = controller
        .withDBSession() { implicit dbSession =>
          controller.userRepository
            .authenticate(controller.user.email, newPassword)
        }
        .awaitResult()
      loginResult must beSome
    }
  }

  "updateUserOrganisation" should {
    "forbidden if user is not assigned to organisation" in new WithUsersControllerMock {
      val request: FakeRequest[UpdateUserOrganisation] = FakeRequest()
        .withBody(
          UpdateUserOrganisation(
            plannedWorkingHours = WorkingHours()
          ))
      val result: Future[Result] =
        controller.updateMyUserOrganisationData(OrganisationId())(request)

      status(result) must equalTo(FORBIDDEN)
    }

    "ok" in new WithUsersControllerMock {
      val workingHours: WorkingHours =
        WorkingHours(monday = 8, tuesday = 4, wednesday = 2, sunday = 1)
      val request: FakeRequest[UpdateUserOrganisation] = FakeRequest()
        .withBody(
          UpdateUserOrganisation(
            plannedWorkingHours = workingHours
          ))
      val result: Future[Result] =
        controller.updateMyUserOrganisationData(controller.organisationId)(
          request)

      status(result) must equalTo(OK)
      val user: UserDTO = contentAsJson(result).as[UserDTO]
      user.organisations
        .find(_.organisationReference.id == controller.organisationId)
        .map(_.plannedWorkingHours) must beSome(workingHours)
    }
  }

  trait WithUsersControllerMock extends WithTestApplication {
    implicit val executionContext: ExecutionContext = inject[ExecutionContext]
    val systemServices: SystemServices              = inject[SystemServices]
    val authConfig: AuthConfig                      = inject[AuthConfig]
    val controller: UsersController
      with SecurityControllerMock
      with MockCacheAware
      with TestDBSupport =
      UsersControllerMock(systemServices, authConfig, reactiveMongoApi)
  }
}

object UsersControllerMock extends MockAwaitable with Mockito with Awaitable {
  def apply(systemServices: SystemServices,
            authConfig: AuthConfig,
            reactiveMongoApi: ReactiveMongoApi)(implicit
      ec: ExecutionContext): UsersController
    with SecurityControllerMock
    with MockCacheAware
    with TestDBSupport = {
    val mongoUserRepository = new UserMongoRepository()

    val controller = new UsersController(Helpers.stubControllerComponents(),
                                         systemServices,
                                         authConfig,
                                         MockCache,
                                         reactiveMongoApi,
                                         mongoUserRepository)
      with SecurityControllerMock
      with MockCacheAware
      with TestDBSupport {
      // override mock as we deal with a real db backend in this spec
      override val userRepository: UserRepository = mongoUserRepository
    }

    controller
      .withDBSession() { implicit dbSession =>
        for {
          // drop previous data
          _ <- mongoUserRepository.dropAll()

          // initialize user
          _ <- mongoUserRepository.upsert(controller.user)
        } yield ()
      }
      .awaitResult()

    controller
  }
}
