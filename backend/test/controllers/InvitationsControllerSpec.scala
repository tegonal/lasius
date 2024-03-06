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
import models.OrganisationId.OrganisationReference
import models.ProjectId.ProjectReference
import models._
import mongo.EmbedMongo
import org.joda.time.DateTime
import org.specs2.mock.Mockito
import org.specs2.mock.mockito.MockitoMatchers
import play.api.mvc._
import play.api.test._

import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class InvitationsControllerSpec
    extends PlaySpecification
    with Mockito
    with Results
    with MockitoMatchers
    with EmbedMongo
    with TestApplication {

  private def createJoinProjectInvitation(
      controller: InvitationsControllerMock)(
      expiration: DateTime = DateTime.now().plusDays(1),
      invitedEmail: String = "someEmail",
      projectReference: ProjectReference = controller.project.reference,
      role: ProjectRole = ProjectMember,
      outcome: Option[InvitationOutcome] = None) = {
    val invitationId = InvitationId()
    withDBSession() { implicit dbSession =>
      controller.invitationRepository.upsert(
        JoinProjectInvitation(
          id = invitationId,
          invitedEmail = invitedEmail,
          createDate = DateTime.now(),
          createdBy = controller.userReference,
          expiration = expiration,
          sharedByOrganisationReference = controller.organisation.reference,
          projectReference = projectReference,
          role = role,
          outcome = outcome
        ))
    }.awaitResult()
    invitationId
  }

  private def createJoinOrganisationInvitation(
      controller: InvitationsControllerMock)(
      expiration: DateTime = DateTime.now().plusDays(1),
      invitedEmail: String = "someEmail",
      organisationReference: OrganisationReference =
        controller.organisation.reference,
      role: OrganisationRole = OrganisationMember,
      outcome: Option[InvitationOutcome] = None) = {
    val invitationId = InvitationId()
    withDBSession() { implicit dbSession =>
      controller.invitationRepository.upsert(
        JoinOrganisationInvitation(
          id = invitationId,
          invitedEmail = invitedEmail,
          createDate = DateTime.now(),
          createdBy = controller.userReference,
          expiration = expiration,
          organisationReference = organisationReference,
          role = role,
          outcome = outcome
        ))
    }.awaitResult()
    invitationId
  }

  "check status" should {

    "badrequest for non existing invitation" in new WithTestApplication {
      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: InvitationsControllerMock =
        controllers.InvitationsControllerMock(systemServices,
                                              authConfig,
                                              reactiveMongoApi)

      val request: FakeRequest[Unit] = FakeRequest()
        .withBody(())
      val result: Future[Result] =
        controller.getStatus(InvitationId())(request)

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo("invitation_not_found")
    }

    "badrequest if invitation is expired" in new WithTestApplication {
      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: InvitationsControllerMock =
        controllers.InvitationsControllerMock(systemServices,
                                              authConfig,
                                              reactiveMongoApi)

      val invitationId =
        createJoinProjectInvitation(controller)(
          expiration = DateTime.now().minusDays(1))

      val request: FakeRequest[Unit] = FakeRequest()
        .withBody(())
      val result: Future[Result] =
        controller.getStatus(invitationId)(request)

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo("invitation_expired")
    }

    "badrequest if invitation was already accepted or declined" in new WithTestApplication {
      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: InvitationsControllerMock =
        controllers.InvitationsControllerMock(systemServices,
                                              authConfig,
                                              reactiveMongoApi)

      val invitationId =
        createJoinProjectInvitation(controller)(
          outcome = Some(
            InvitationOutcome(userReference = controller.userReference,
                              dateTime = DateTime.now(),
                              status = InvitationAccepted)))

      val request: FakeRequest[Unit] = FakeRequest()
        .withBody(())
      val result: Future[Result] =
        controller.getStatus(invitationId)(request)

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo("invitation_invalid_state")
    }

    "badrequest if invited user was deactivated in the meantime" in new WithTestApplication {
      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: InvitationsControllerMock =
        controllers.InvitationsControllerMock(systemServices,
                                              authConfig,
                                              reactiveMongoApi,
                                              userActive = false)

      val invitationId =
        createJoinProjectInvitation(controller)(
          invitedEmail = controller.user.email)

      val request: FakeRequest[Unit] = FakeRequest()
        .withBody(())
      val result: Future[Result] =
        controller.getStatus(invitationId)(request)

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo("user_deactivated")
    }

    "ok with status UnregisteredUser if user does not exist" in new WithTestApplication {
      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: InvitationsControllerMock =
        controllers.InvitationsControllerMock(systemServices,
                                              authConfig,
                                              reactiveMongoApi)

      val invitationId =
        createJoinProjectInvitation(controller)()

      val request: FakeRequest[Unit] = FakeRequest()
        .withBody(())
      val result: Future[Result] =
        controller.getStatus(invitationId)(request)

      status(result) must equalTo(OK)

      val response = contentAsJson(result)
        .as[InvitationStatusResponse]
      response.status === UnregisteredUser
      response.invitation.id === invitationId
    }

    "ok with status InvitationOk if user does already exist" in new WithTestApplication {
      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: InvitationsControllerMock =
        controllers.InvitationsControllerMock(systemServices,
                                              authConfig,
                                              reactiveMongoApi)

      val invitationId =
        createJoinProjectInvitation(controller)(
          invitedEmail = controller.user.email)

      val request: FakeRequest[Unit] = FakeRequest()
        .withBody(())
      val result: Future[Result] =
        controller.getStatus(invitationId)(request)

      status(result) must equalTo(OK)
      val response = contentAsJson(result)
        .as[InvitationStatusResponse]
      response.status === InvitationOk
      response.invitation.id === invitationId
    }
  }

  "register user" should {
    "badrequest for non existing invitation" in new WithTestApplication {
      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: InvitationsControllerMock =
        controllers.InvitationsControllerMock(systemServices,
                                              authConfig,
                                              reactiveMongoApi)

      val request: FakeRequest[UserRegistration] = FakeRequest()
        .withBody(
          UserRegistration(key = "someUserKey",
                           password = "myPwd",
                           firstName = "firstName",
                           lastName = "lastName",
                           plannedWorkingHours = None))
      val result: Future[Result] =
        controller.registerUser(InvitationId())(request)

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo("invitation_not_found")
    }

    "badrequest if provided user key is blank" in new WithTestApplication {
      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: InvitationsControllerMock =
        controllers.InvitationsControllerMock(systemServices,
                                              authConfig,
                                              reactiveMongoApi)

      val invitationId =
        createJoinProjectInvitation(controller)()

      val request: FakeRequest[UserRegistration] = FakeRequest()
        .withBody(
          UserRegistration(key = "",
                           password = "myPwd",
                           firstName = "firstName",
                           lastName = "lastName",
                           plannedWorkingHours = None))
      val result: Future[Result] =
        controller.registerUser(invitationId)(request)

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo(
        "expected non-blank String for field 'key'")
    }

    "badrequest if user key already exist" in new WithTestApplication {
      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: InvitationsControllerMock =
        controllers.InvitationsControllerMock(systemServices,
                                              authConfig,
                                              reactiveMongoApi)

      val invitationId =
        createJoinProjectInvitation(controller)()

      val request: FakeRequest[UserRegistration] = FakeRequest()
        .withBody(
          UserRegistration(key = controller.userKey,
                           password = "myPwd",
                           firstName = "firstName",
                           lastName = "lastName",
                           plannedWorkingHours = None))
      val result: Future[Result] =
        controller.registerUser(invitationId)(request)

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo("user_key_already_exists")
    }

    "badrequest if user email already exist" in new WithTestApplication {
      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: InvitationsControllerMock =
        controllers.InvitationsControllerMock(systemServices,
                                              authConfig,
                                              reactiveMongoApi)

      val invitationId =
        createJoinProjectInvitation(controller)(
          invitedEmail = controller.user.email)

      val request: FakeRequest[UserRegistration] = FakeRequest()
        .withBody(
          UserRegistration(key = "someNewKey",
                           password = "myPwd",
                           firstName = "firstName",
                           lastName = "lastName",
                           plannedWorkingHours = None))
      val result: Future[Result] =
        controller.registerUser(invitationId)(request)

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo("user_already_registered")
    }

    "badrequest if organisation with user key already exists" in new WithTestApplication {
      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: InvitationsControllerMock =
        controllers.InvitationsControllerMock(systemServices,
                                              authConfig,
                                              reactiveMongoApi)

      val invitationId =
        createJoinProjectInvitation(controller)()
      val newUserKey = "newUserKey"

      withDBSession()(implicit dbSession =>
        controller.organisationRepository.upsert(
          Organisation(id = OrganisationId(),
                       key = newUserKey,
                       `private` = true,
                       active = true,
                       createdBy = controller.userReference,
                       deactivatedBy = None))).awaitResult()

      val request: FakeRequest[UserRegistration] = FakeRequest()
        .withBody(
          UserRegistration(key = newUserKey,
                           password = "dsa33ffffLL",
                           firstName = "firstName",
                           lastName = "lastName",
                           plannedWorkingHours = None))
      val result: Future[Result] =
        controller.registerUser(invitationId)(request)

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo(
        s"Cannot create organisation with same key $newUserKey")
    }

    "badrequest if password does not match password policy" in new WithTestApplication {
      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: InvitationsControllerMock =
        controllers.InvitationsControllerMock(systemServices,
                                              authConfig,
                                              reactiveMongoApi)

      val invitationId =
        createJoinProjectInvitation(controller)()

      val request: FakeRequest[UserRegistration] = FakeRequest()
        .withBody(
          UserRegistration(key = "someNewKey",
                           password = "1",
                           firstName = "firstName",
                           lastName = "lastName",
                           plannedWorkingHours = None))
      val result: Future[Result] =
        controller.registerUser(invitationId)(request)

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo("password policy not satisfied")
    }

    "successful user created with private organisation" in new WithTestApplication {
      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: InvitationsControllerMock =
        controllers.InvitationsControllerMock(systemServices,
                                              authConfig,
                                              reactiveMongoApi)
      val email = "testUser@lasius.ch"

      val invitationId =
        createJoinProjectInvitation(controller)(invitedEmail = email)
      val password = "ddFFf33s222SS"

      val request: FakeRequest[UserRegistration] = FakeRequest()
        .withBody(
          UserRegistration(key = "someNewUserKey",
                           password = password,
                           firstName = "firstName",
                           lastName = "lastName",
                           plannedWorkingHours = None))
      val result: Future[Result] =
        controller.registerUser(invitationId)(request)

      status(result) must equalTo(OK)
      val invitation = contentAsJson(result).as[Invitation]

      val user = withDBSession()(implicit dbSession =>
        controller.userRepository.findByEmail(invitation.invitedEmail))
        .awaitResult()
      user must beSome

      val organisation = withDBSession()(implicit dbSession =>
        controller.organisationRepository.findByKey(user.get.key)).awaitResult()
      organisation must beSome
      organisation.get.`private` === true

      // validate user is assigned to newly created organisation as administrator
      val userOrg = user.get.organisations.find(
        _.organisationReference == organisation.get.reference)
      userOrg must beSome
      userOrg.get.role === OrganisationAdministrator

      // validate user can log in
      val loginResult: Option[User] = controller
        .withDBSession() { implicit dbSession =>
          controller.userRepository
            .authenticate(email, password)
        }
        .awaitResult()
      loginResult must beSome
    }
  }

  "decline invitation" should {
    "badrequest for non existing invitation" in new WithTestApplication {
      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: InvitationsControllerMock =
        controllers.InvitationsControllerMock(systemServices,
                                              authConfig,
                                              reactiveMongoApi)

      val request: FakeRequest[Unit] = FakeRequest()
        .withBody(())
      val result: Future[Result] =
        controller.decline(InvitationId())(request)

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo("invitation_not_found")
    }

    "badrequest if logged in user does not match invited email" in new WithTestApplication {
      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: InvitationsControllerMock =
        controllers.InvitationsControllerMock(systemServices,
                                              authConfig,
                                              reactiveMongoApi)

      val invitationId =
        createJoinProjectInvitation(controller)(invitedEmail = "someOtherEmail")

      val request: FakeRequest[Unit] = FakeRequest()
        .withBody(())
      val result: Future[Result] =
        controller.decline(invitationId)(request)

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo("illegal_access")
    }

    "successful initation declined" in new WithTestApplication {
      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: InvitationsControllerMock =
        controllers.InvitationsControllerMock(systemServices,
                                              authConfig,
                                              reactiveMongoApi)

      val invitationId =
        createJoinProjectInvitation(controller)(
          invitedEmail = controller.user.email)

      val request: FakeRequest[Unit] = FakeRequest()
        .withBody(())
      val result: Future[Result] =
        controller.decline(invitationId)(request)

      status(result) must equalTo(OK)
      val invitation = contentAsJson(result).as[Invitation]
      invitation.outcome must beSome
      invitation.outcome.get.status === InvitationDeclined
    }
  }

  "accept invitation" should {
    "badrequest for non existing invitation" in new WithTestApplication {
      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: InvitationsControllerMock =
        controllers.InvitationsControllerMock(systemServices,
                                              authConfig,
                                              reactiveMongoApi)

      val request: FakeRequest[AcceptInvitationRequest] = FakeRequest()
        .withBody(AcceptInvitationRequest(organisationReference = None))
      val result: Future[Result] =
        controller.accept(InvitationId())(request)

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo("invitation_not_found")
    }

    "badrequest if logged in user does not match invited email" in new WithTestApplication {
      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: InvitationsControllerMock =
        controllers.InvitationsControllerMock(systemServices,
                                              authConfig,
                                              reactiveMongoApi)

      val invitationId =
        createJoinProjectInvitation(controller)(invitedEmail = "someOtherEmail")

      val request: FakeRequest[AcceptInvitationRequest] = FakeRequest()
        .withBody(AcceptInvitationRequest(organisationReference = None))
      val result: Future[Result] =
        controller.accept(invitationId)(request)

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo("illegal_access")
    }

    "forbidden if user is not a member of the provided organisation" in new WithTestApplication {
      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: InvitationsControllerMock =
        controllers.InvitationsControllerMock(systemServices,
                                              authConfig,
                                              reactiveMongoApi)

      val invitationId =
        createJoinProjectInvitation(controller)(invitedEmail = "someOtherEmail")

      val request: FakeRequest[AcceptInvitationRequest] = FakeRequest()
        .withBody(
          AcceptInvitationRequest(organisationReference =
            Some(EntityReference(OrganisationId(), "someKey"))))
      val result: Future[Result] =
        controller.accept(invitationId)(request)

      status(result) must equalTo(FORBIDDEN)
    }

    "for JoinProjectInvitation" in {
      "badrequest if no organisationReference was provided" in new WithTestApplication {
        implicit val executionContext: ExecutionContext =
          inject[ExecutionContext]
        val systemServices: SystemServices = inject[SystemServices]
        val authConfig: AuthConfig         = inject[AuthConfig]
        val controller: InvitationsControllerMock =
          controllers.InvitationsControllerMock(systemServices,
                                                authConfig,
                                                reactiveMongoApi)

        val invitationId =
          createJoinProjectInvitation(controller)(
            invitedEmail = controller.user.email)

        val request: FakeRequest[AcceptInvitationRequest] = FakeRequest()
          .withBody(AcceptInvitationRequest(organisationReference = None))
        val result: Future[Result] =
          controller.accept(invitationId)(request)

        status(result) must equalTo(BAD_REQUEST)
        contentAsString(result) must equalTo(
          "Need to specify binding organisation when joining a project")
      }

      "badrequest if user is already assigned to project and organisation" in new WithTestApplication {
        implicit val executionContext: ExecutionContext =
          inject[ExecutionContext]
        val systemServices: SystemServices = inject[SystemServices]
        val authConfig: AuthConfig         = inject[AuthConfig]
        val controller: InvitationsControllerMock =
          controllers.InvitationsControllerMock(systemServices,
                                                authConfig,
                                                reactiveMongoApi)

        val invitationId =
          createJoinProjectInvitation(controller)(
            invitedEmail = controller.user.email)

        val request: FakeRequest[AcceptInvitationRequest] = FakeRequest()
          .withBody(
            AcceptInvitationRequest(organisationReference =
              Some(controller.organisation.reference)))
        val result: Future[Result] =
          controller.accept(invitationId)(request)

        status(result) must equalTo(BAD_REQUEST)
        contentAsString(result) must equalTo(
          s"User already assigned to project ${controller.project.key} and organisation ${controller.organisation.key}")
      }

      "badrequest project does not exist" in new WithTestApplication {
        implicit val executionContext: ExecutionContext =
          inject[ExecutionContext]
        val systemServices: SystemServices = inject[SystemServices]
        val authConfig: AuthConfig         = inject[AuthConfig]
        val controller: InvitationsControllerMock =
          controllers.InvitationsControllerMock(systemServices,
                                                authConfig,
                                                reactiveMongoApi)

        val invitationId =
          createJoinProjectInvitation(controller)(
            invitedEmail = controller.user.email,
            projectReference =
              EntityReference(ProjectId(), "nonExistingProject"))

        val request: FakeRequest[AcceptInvitationRequest] = FakeRequest()
          .withBody(
            AcceptInvitationRequest(organisationReference =
              Some(controller.organisation.reference)))
        val result: Future[Result] =
          controller.accept(invitationId)(request)

        status(result) must equalTo(BAD_REQUEST)
        contentAsString(result) must equalTo(
          s"Project nonExistingProject does not exist")
      }

      "badrequest if project is inactive" in new WithTestApplication {
        implicit val executionContext: ExecutionContext =
          inject[ExecutionContext]
        val systemServices: SystemServices = inject[SystemServices]
        val authConfig: AuthConfig         = inject[AuthConfig]
        val controller: InvitationsControllerMock =
          controllers.InvitationsControllerMock(systemServices,
                                                authConfig,
                                                reactiveMongoApi)

        val project = Project(
          id = ProjectId(),
          key = "newProject",
          organisationReference = controller.organisation.reference,
          bookingCategories = Set(),
          active = false,
          createdBy = controller.userReference,
          deactivatedBy = None
        )
        withDBSession()(implicit dbSession =>
          controller.projectRepository.upsert(project)).awaitResult()

        val invitationId =
          createJoinProjectInvitation(controller)(invitedEmail =
                                                    controller.user.email,
                                                  projectReference =
                                                    project.reference)

        val request: FakeRequest[AcceptInvitationRequest] = FakeRequest()
          .withBody(
            AcceptInvitationRequest(organisationReference =
              Some(controller.organisation.reference)))
        val result: Future[Result] =
          controller.accept(invitationId)(request)

        status(result) must equalTo(BAD_REQUEST)
        contentAsString(result) must equalTo(
          s"Cannot join inactive project newProject")
      }

      "successful user assigned to project in provided organisation with correct role" in new WithTestApplication {
        implicit val executionContext: ExecutionContext =
          inject[ExecutionContext]
        val systemServices: SystemServices = inject[SystemServices]
        val authConfig: AuthConfig         = inject[AuthConfig]
        val controller: InvitationsControllerMock =
          controllers.InvitationsControllerMock(systemServices,
                                                authConfig,
                                                reactiveMongoApi)

        val project = Project(
          id = ProjectId(),
          key = "newProject",
          organisationReference = controller.organisation.reference,
          bookingCategories = Set(),
          active = true,
          createdBy = controller.userReference,
          deactivatedBy = None
        )
        withDBSession()(implicit dbSession =>
          controller.projectRepository.upsert(project)).awaitResult()

        val invitationId =
          createJoinProjectInvitation(controller)(invitedEmail =
                                                    controller.user.email,
                                                  projectReference =
                                                    project.reference,
                                                  role = ProjectMember)

        val request: FakeRequest[AcceptInvitationRequest] = FakeRequest()
          .withBody(
            AcceptInvitationRequest(organisationReference =
              Some(controller.organisation.reference)))
        val result: Future[Result] =
          controller.accept(invitationId)(request)

        status(result) must equalTo(OK)
        val invitation = contentAsJson(result).as[Invitation]
        invitation.outcome must beSome
        invitation.outcome.get.status === InvitationAccepted

        // verify user is assigned to project with correct role
        val user = withDBSession()(implicit dbSession =>
          controller.userRepository.findById(controller.userId))
          .awaitResult()
          .get

        val userOrg = user.organisations
          .find(_.organisationReference.id == controller.organisationId)
          .get
        val userProject =
          userOrg.projects.find(_.projectReference.id == project.id)

        userProject must beSome
        userProject.get.role === ProjectMember
      }
    }

    "for JoinOrganisationInvitation" in {
      "badrequest organisation does not exist" in new WithTestApplication {
        implicit val executionContext: ExecutionContext =
          inject[ExecutionContext]
        val systemServices: SystemServices = inject[SystemServices]
        val authConfig: AuthConfig         = inject[AuthConfig]
        val controller: InvitationsControllerMock =
          controllers.InvitationsControllerMock(systemServices,
                                                authConfig,
                                                reactiveMongoApi)

        val invitationId =
          createJoinOrganisationInvitation(controller)(
            invitedEmail = controller.user.email,
            organisationReference =
              EntityReference(OrganisationId(), "nonExistingOrganisation"))

        val request: FakeRequest[AcceptInvitationRequest] = FakeRequest()
          .withBody(AcceptInvitationRequest(organisationReference = None))
        val result: Future[Result] =
          controller.accept(invitationId)(request)

        status(result) must equalTo(BAD_REQUEST)
        contentAsString(result) must equalTo(
          s"Organisation nonExistingOrganisation does not exist")
      }

      "badrequest if organisation is inactive" in new WithTestApplication {
        implicit val executionContext: ExecutionContext =
          inject[ExecutionContext]
        val systemServices: SystemServices = inject[SystemServices]
        val authConfig: AuthConfig         = inject[AuthConfig]
        val controller: InvitationsControllerMock =
          controllers.InvitationsControllerMock(systemServices,
                                                authConfig,
                                                reactiveMongoApi)

        val organisation = Organisation(
          id = OrganisationId(),
          key = "newOrganisation",
          `private` = false,
          active = false,
          createdBy = controller.userReference,
          deactivatedBy = None
        )
        withDBSession()(implicit dbSession =>
          controller.organisationRepository.upsert(organisation)).awaitResult()

        val invitationId =
          createJoinOrganisationInvitation(controller)(
            invitedEmail = controller.user.email,
            organisationReference = organisation.reference
          )

        val request: FakeRequest[AcceptInvitationRequest] = FakeRequest()
          .withBody(AcceptInvitationRequest(organisationReference = None))
        val result: Future[Result] =
          controller.accept(invitationId)(request)

        status(result) must equalTo(BAD_REQUEST)
        contentAsString(result) must equalTo(
          s"Cannot join inactive organisation newOrganisation")
      }

      "successful user assigned to organisation with correct role" in new WithTestApplication {
        implicit val executionContext: ExecutionContext =
          inject[ExecutionContext]
        val systemServices: SystemServices = inject[SystemServices]
        val authConfig: AuthConfig         = inject[AuthConfig]
        val controller: InvitationsControllerMock =
          controllers.InvitationsControllerMock(systemServices,
                                                authConfig,
                                                reactiveMongoApi)

        val organisation = Organisation(
          id = OrganisationId(),
          key = "newOrganisation",
          `private` = false,
          active = true,
          createdBy = controller.userReference,
          deactivatedBy = None
        )
        withDBSession()(implicit dbSession =>
          controller.organisationRepository.upsert(organisation)).awaitResult()

        val invitationId =
          createJoinOrganisationInvitation(controller)(
            invitedEmail = controller.user.email,
            organisationReference = organisation.reference
          )

        val request: FakeRequest[AcceptInvitationRequest] = FakeRequest()
          .withBody(AcceptInvitationRequest(organisationReference = None))
        val result: Future[Result] =
          controller.accept(invitationId)(request)

        status(result) must equalTo(OK)
        val invitation = contentAsJson(result).as[Invitation]
        invitation.outcome must beSome
        invitation.outcome.get.status === InvitationAccepted

        // verify user is assigned to organisation with correct role
        val user = withDBSession()(implicit dbSession =>
          controller.userRepository.findById(controller.userId))
          .awaitResult()
          .get

        val userOrg = user.organisations
          .find(_.organisationReference.id == organisation.id)
        userOrg must beSome
        userOrg.get.role === OrganisationMember
      }
    }
  }
}
