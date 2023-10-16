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

import core.{SystemServices, TestApplication}
import models._
import mongo.EmbedMongo
import org.joda.time.DateTime
import org.mindrot.jbcrypt.BCrypt
import org.specs2.mock.Mockito
import org.specs2.mock.mockito.MockitoMatchers
import play.api.mvc._
import play.api.test._

import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class OrganisationsControllerSpec
    extends PlaySpecification
    with Mockito
    with Results
    with MockitoMatchers
    with EmbedMongo
    with TestApplication {

  "create organisation" should {

    "badrequest blank key was specified" in new WithTestApplication {

      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: OrganisationsControllerMock =
        controllers.OrganisationsControllerMock(systemServices,
                                                authConfig,
                                                reactiveMongoApi)

      val request: FakeRequest[CreateOrganisation] = FakeRequest()
        .withBody(
          CreateOrganisation(
            key = "",
            plannedWorkingHours = None
          ))
      val result: Future[Result] =
        controller.createOrganisation()(request)

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo(
        "expected non-blank String for field 'key'")
    }

    "badrequest creating organisation with an existing key" in new WithTestApplication {

      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: OrganisationsControllerMock =
        controllers.OrganisationsControllerMock(systemServices,
                                                authConfig,
                                                reactiveMongoApi)

      val request: FakeRequest[CreateOrganisation] = FakeRequest()
        .withBody(
          CreateOrganisation(
            key = controller.organisation.key,
            plannedWorkingHours = None
          ))
      val result: Future[Result] =
        controller.createOrganisation()(request)

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo(
        s"Cannot create organisation with same key ${controller.organisation.key}")
    }

    "successful, user assigned to new organisation as OrganisationAdministrator" in new WithTestApplication {

      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: OrganisationsControllerMock =
        controllers.OrganisationsControllerMock(systemServices,
                                                authConfig,
                                                reactiveMongoApi)
      val newOrganisationKey: String = "someGreateOrg"

      val request: FakeRequest[CreateOrganisation] = FakeRequest()
        .withBody(
          CreateOrganisation(
            key = newOrganisationKey,
            plannedWorkingHours = None
          ))
      val result: Future[Result] =
        controller.createOrganisation()(request)

      status(result) must equalTo(CREATED)
      val resultingOrganisation = contentAsJson(result).as[Organisation]
      resultingOrganisation.key === newOrganisationKey

      // verify user gets automatically assigned to this project
      val maybeUser = withDBSession()(implicit dbSession =>
        controller.userRepository.findByUserReference(controller.userReference))
        .awaitResult()
      maybeUser must beSome
      val user = maybeUser.get
      val userOrg = user.organisations.find(
        _.organisationReference == resultingOrganisation.getReference())
      userOrg must beSome
      userOrg.get.role === OrganisationAdministrator
    }
  }

  "deactivate organisation" should {
    "Unauthorized if organisation id does not exist" in new WithTestApplication {

      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: OrganisationsControllerMock =
        controllers.OrganisationsControllerMock(systemServices,
                                                authConfig,
                                                reactiveMongoApi)
      val newOrganisationId = OrganisationId()

      val request: FakeRequest[Unit] = FakeRequest().withBody(())
      val result: Future[Result] =
        controller.deactivateOrganisation(newOrganisationId)(request)

      status(result) must equalTo(UNAUTHORIZED)
    }

    "successful, organisation unassigned from all users" in new WithTestApplication {

      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: OrganisationsControllerMock =
        controllers.OrganisationsControllerMock(systemServices,
                                                authConfig,
                                                reactiveMongoApi)
      val request: FakeRequest[Unit] = FakeRequest().withBody(())
      val result: Future[Result] =
        controller.deactivateOrganisation(controller.organisationId)(request)

      status(result) must equalTo(OK)

      // verify user gets unassigned from project
      val maybeUser = withDBSession()(implicit dbSession =>
        controller.userRepository.findByUserReference(controller.userReference))
        .awaitResult()
      maybeUser must beSome
      val user = maybeUser.get
      val userOrg = user.organisations.find(
        _.organisationReference.id == controller.organisationId)
      userOrg must beNone
    }
  }

  "invite user to organisation" should {
    "badrequest if incorrect email was provided" in new WithTestApplication {

      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: OrganisationsControllerMock =
        controllers.OrganisationsControllerMock(systemServices,
                                                authConfig,
                                                reactiveMongoApi)

      val request: FakeRequest[UserToOrganisationAssignment] =
        FakeRequest().withBody(
          UserToOrganisationAssignment(email = "noEmail",
                                       role = OrganisationMember))
      val result: Future[Result] =
        controller.inviteUser(controller.organisationId)(request)

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo(
        s"Not a valid email address 'noEmail'")
    }

    "badrequest if organisation does not exist" in new WithTestApplication {

      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: OrganisationsControllerMock =
        controllers.OrganisationsControllerMock(systemServices,
                                                authConfig,
                                                reactiveMongoApi)
      val email = "newUserEmail@test.com"

      // delete organisation
      withDBSession()(implicit dbSession =>
        controller.organisationRepository.removeById(controller.organisationId))
        .awaitResult()

      val request: FakeRequest[UserToOrganisationAssignment] =
        FakeRequest().withBody(
          UserToOrganisationAssignment(email = email,
                                       role = OrganisationMember))
      val result: Future[Result] =
        controller.inviteUser(controller.organisationId)(request)

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo(
        s"Organisation ${controller.organisation.key} does not exist")
    }

    "badrequest if organisation is inactive" in new WithTestApplication {

      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: OrganisationsControllerMock =
        controllers.OrganisationsControllerMock(systemServices,
                                                authConfig,
                                                reactiveMongoApi,
                                                organisationActive = false)
      val email = "newUserEmail@test.com"

      val request: FakeRequest[UserToOrganisationAssignment] =
        FakeRequest().withBody(
          UserToOrganisationAssignment(email = email,
                                       role = OrganisationMember))
      val result: Future[Result] =
        controller.inviteUser(controller.organisationId)(request)

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo(
        s"Cannot invite to an inactive organisation ${controller.organisation.key}")
    }

    "successful, invitation created" in new WithTestApplication {

      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: OrganisationsControllerMock =
        controllers.OrganisationsControllerMock(systemServices,
                                                authConfig,
                                                reactiveMongoApi)
      val email = "newUserEmail@test.com"

      val request: FakeRequest[UserToOrganisationAssignment] =
        FakeRequest().withBody(
          UserToOrganisationAssignment(email = email,
                                       role = OrganisationMember))
      val result: Future[Result] =
        controller.inviteUser(controller.organisationId)(request)

      status(result) must equalTo(CREATED)
      val invitationResult: InvitationResult =
        contentAsJson(result).as[InvitationResult]
      invitationResult.email === email
    }
  }

  "remove other user from organisation" should {
    "unauthorized if user is not assigned as Administrator to organisation" in new WithTestApplication {

      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: OrganisationsControllerMock =
        controllers.OrganisationsControllerMock(systemServices,
                                                authConfig,
                                                reactiveMongoApi,
                                                OrganisationMember)

      val request: FakeRequest[Unit] = FakeRequest().withBody(())
      val result: Future[Result] =
        controller.unassignUser(controller.organisationId, UserId())(request)

      status(result) must equalTo(UNAUTHORIZED)
    }

    "badrequest if user is single active organisation administrator" in new WithTestApplication {

      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: OrganisationsControllerMock =
        controllers.OrganisationsControllerMock(systemServices,
                                                authConfig,
                                                reactiveMongoApi)

      val request: FakeRequest[Unit] = FakeRequest().withBody(())
      val result: Future[Result] =
        controller.unassignUser(controller.organisationId, controller.userId)(
          request)

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo(
        s"RemovalDenied.UserIsLastUserReference")
    }

    "badrequest if user want's to remove himself from his private organisation" in new WithTestApplication {

      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: OrganisationsControllerMock =
        controllers.OrganisationsControllerMock(systemServices,
                                                authConfig,
                                                reactiveMongoApi,
                                                isOrganisationPrivate = true)

      val request: FakeRequest[Unit] = FakeRequest().withBody(())
      val result: Future[Result] =
        controller.unassignUser(controller.organisationId, controller.userId)(
          request)

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo(
        s"Cannot remove user from own private organisation")
    }

    "successful" in new WithTestApplication {

      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: OrganisationsControllerMock =
        controllers.OrganisationsControllerMock(systemServices,
                                                authConfig,
                                                reactiveMongoApi)

      // initialize second user
      val userProject2 = UserProject(
        sharedByOrganisationReference = None,
        projectReference = controller.project.getReference(),
        role = ProjectMember
      )
      val userOrganisation2 = UserOrganisation(
        organisationReference = controller.organisation.getReference(),
        `private` = controller.organisation.`private`,
        role = OrganisationMember,
        plannedWorkingHours = WorkingHours(),
        projects = Seq(userProject2)
      )
      val user2: User = User(
        UserId(),
        "anotherUser",
        email = "user2@user.com",
        password = BCrypt.hashpw("somePassword", BCrypt.gensalt()),
        firstName = "test",
        lastName = "user",
        active = true,
        role = Administrator,
        organisations = Seq(userOrganisation2),
        settings = None
      )
      withDBSession()(implicit dbSession =>
        controller.userRepository.upsert(user2)).awaitResult()

      val request: FakeRequest[Unit] = FakeRequest().withBody(())
      val result: Future[Result] =
        controller.unassignUser(controller.organisationId, user2.id)(request)

      status(result) must equalTo(OK)

      val remainingUsers = withDBSession()(implicit dbSession =>
        controller.userRepository.findByOrganisation(
          controller.organisation.getReference()))
        .awaitResult()
      remainingUsers should haveSize(1)
    }
  }

  "remove own user from organisation" should {
    "unauthorized if user is not assigned to organisation" in new WithTestApplication {

      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: OrganisationsControllerMock =
        controllers.OrganisationsControllerMock(systemServices,
                                                authConfig,
                                                reactiveMongoApi)

      val request: FakeRequest[Unit] = FakeRequest().withBody(())
      val result: Future[Result] =
        controller.unassignMyUser(OrganisationId())(request)

      status(result) must equalTo(UNAUTHORIZED)
    }

    "badrequest if user is single active organisation administrator" in new WithTestApplication {

      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: OrganisationsControllerMock =
        controllers.OrganisationsControllerMock(systemServices,
                                                authConfig,
                                                reactiveMongoApi)

      val request: FakeRequest[Unit] = FakeRequest().withBody(())
      val result: Future[Result] =
        controller.unassignMyUser(controller.organisationId)(request)

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo(
        s"RemovalDenied.UserIsLastUserReference")
    }

    "badrequest if user want's to remove himself from his private organisation" in new WithTestApplication {

      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: OrganisationsControllerMock =
        controllers.OrganisationsControllerMock(systemServices,
                                                authConfig,
                                                reactiveMongoApi,
                                                isOrganisationPrivate = true)

      val request: FakeRequest[Unit] = FakeRequest().withBody(())
      val result: Future[Result] =
        controller.unassignMyUser(controller.organisationId)(request)

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo(
        s"Cannot remove user from own private organisation")
    }

    "successful as OrganisationMember" in new WithTestApplication {

      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: OrganisationsControllerMock =
        controllers.OrganisationsControllerMock(systemServices,
                                                authConfig,
                                                reactiveMongoApi,
                                                OrganisationMember)

      // initialize second user to be able to remove ourself
      val userProject2 = UserProject(
        sharedByOrganisationReference = None,
        projectReference = controller.project.getReference(),
        role = ProjectAdministrator
      )
      val userOrganisation2 = UserOrganisation(
        organisationReference = controller.organisation.getReference(),
        `private` = controller.organisation.`private`,
        role = OrganisationAdministrator,
        plannedWorkingHours = WorkingHours(),
        projects = Seq(userProject2)
      )
      val user2: User = User(
        UserId(),
        "anotherUser",
        email = "user2@user.com",
        password = BCrypt.hashpw("somePassword", BCrypt.gensalt()),
        firstName = "test",
        lastName = "user",
        active = true,
        role = Administrator,
        organisations = Seq(userOrganisation2),
        settings = None
      )
      withDBSession()(implicit dbSession =>
        controller.userRepository.upsert(user2)).awaitResult()

      val request: FakeRequest[Unit] = FakeRequest().withBody(())
      val result: Future[Result] =
        controller.unassignMyUser(controller.organisationId)(request)

      status(result) must equalTo(OK)
      val remainingUsers = withDBSession()(implicit dbSession =>
        controller.userRepository.findByOrganisation(
          controller.organisation.getReference()))
        .awaitResult()
      remainingUsers should haveSize(1)
    }
  }

  "update organisaton" should {
    "badrequest no update was provided" in new WithTestApplication {

      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: OrganisationsControllerMock =
        controllers.OrganisationsControllerMock(systemServices,
                                                authConfig,
                                                reactiveMongoApi)

      val request: FakeRequest[UpdateOrganisation] =
        FakeRequest().withBody(UpdateOrganisation(key = None))
      val result: Future[Result] =
        controller.updateOrganisation(controller.organisationId)(request)

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo(
        s"cannot update organisation ${controller.organisation.key}, at least one field must be specified")
    }

    "badrequest update with empty key was provided" in new WithTestApplication {

      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: OrganisationsControllerMock =
        controllers.OrganisationsControllerMock(systemServices,
                                                authConfig,
                                                reactiveMongoApi)

      val request: FakeRequest[UpdateOrganisation] =
        FakeRequest().withBody(UpdateOrganisation(key = Some("")))
      val result: Future[Result] =
        controller.updateOrganisation(controller.organisationId)(request)

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo(
        s"expected non-blank String for field 'key'")
    }

    "badrequest if duplicate organisation key was provided" in new WithTestApplication {

      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: OrganisationsControllerMock =
        controllers.OrganisationsControllerMock(systemServices,
                                                authConfig,
                                                reactiveMongoApi)

      val organisation2Key: String = "org2"

      // create second project in same organisation
      withDBSession()(implicit dbSession =>
        controller.organisationRepository.upsert(
          Organisation(
            id = OrganisationId(),
            key = organisation2Key,
            `private` = false,
            active = true,
            createdBy = controller.userReference,
            deactivatedBy = None
          ))).awaitResult()

      val request: FakeRequest[UpdateOrganisation] =
        FakeRequest().withBody(UpdateOrganisation(key = Some(organisation2Key)))
      val result: Future[Result] =
        controller.updateOrganisation(controller.organisationId)(request)

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo(
        s"Cannot create organisation with same key ${organisation2Key}")
    }

    "successful updated key in all references of main-entities" in new WithTestApplication {

      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: OrganisationsControllerMock =
        controllers.OrganisationsControllerMock(systemServices,
                                                authConfig,
                                                reactiveMongoApi)
      val newKey       = "newOrgKey"
      val invitationId = InvitationId()

      // create second user with different project structure
      val anotherUser: User = User(
        UserId(),
        "second-user",
        email = "user@user.com",
        password = BCrypt.hashpw("no-pwd", BCrypt.gensalt()),
        firstName = "test",
        lastName = "user",
        active = true,
        role = Administrator,
        organisations = Seq(
          UserOrganisation(
            organisationReference = controller.organisation.getReference(),
            `private` = false,
            role = OrganisationMember,
            plannedWorkingHours = WorkingHours(),
            projects = Seq()
          ),
          UserOrganisation(
            organisationReference = EntityReference(OrganisationId(), "myOrg"),
            `private` = true,
            role = OrganisationAdministrator,
            plannedWorkingHours = WorkingHours(),
            projects = Seq()
          )
        ),
        settings = None
      )

      val invitation = JoinOrganisationInvitation(
        id = invitationId,
        invitedEmail = "someEmail",
        createDate = DateTime.now(),
        createdBy = controller.userReference,
        expiration = DateTime.now().plusDays(1),
        organisationReference = controller.organisation.getReference(),
        role = OrganisationMember,
        outcome = None
      )

      withDBSession() { implicit dbSession =>
        // create invitation
        for {
          _ <- controller.userRepository.upsert(anotherUser)
          _ <- controller.invitationRepository.upsert(invitation)
        } yield ()
      }.awaitResult()

      val request: FakeRequest[UpdateOrganisation] =
        FakeRequest().withBody(UpdateOrganisation(key = Some(newKey)))
      val result: Future[Result] =
        controller.updateOrganisation(controller.organisationId)(request)

      status(result) must equalTo(OK)
      val updatedOrganisation = contentAsJson(result).as[Organisation]

      updatedOrganisation.key === newKey

      // verify references where updated as well
      val updatedInvitation = withDBSession()(implicit dbSession =>
        controller.invitationRepository.findById(invitationId)).awaitResult()
      updatedInvitation must beSome
      updatedInvitation.get
        .asInstanceOf[JoinOrganisationInvitation]
        .organisationReference
        .key === newKey

      val user = withDBSession()(implicit dbSession =>
        controller.userRepository.findById(controller.userId)).awaitResult()
      user must beSome

      user.get.organisations
        .find(_.organisationReference.id == controller.organisationId)
        .map(_.organisationReference.key) === Some(newKey)

      val user2 = withDBSession()(implicit dbSession =>
        controller.userRepository.findById(controller.userId)).awaitResult()
      user2 must beSome

      user2.get.organisations
        .find(_.organisationReference.id == controller.organisationId)
        .map(_.organisationReference.key) === Some(newKey)

      user2.get.organisations
        .filter(_.organisationReference.id != controller.organisationId)
        .map(_.organisationReference.key) must not contain (newKey)
    }
  }
}
