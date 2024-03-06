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

class ProjectsControllerSpec
    extends PlaySpecification
    with Mockito
    with Results
    with MockitoMatchers
    with EmbedMongo
    with TestApplication {

  "create project" should {

    "forbidden create project in organisation not assigned to user" in new WithTestApplication {

      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: ProjectsControllerMock =
        controllers.ProjectsControllerMock(systemServices,
                                           authConfig,
                                           reactiveMongoApi)

      val request: FakeRequest[CreateProject] = FakeRequest()
        .withBody(
          CreateProject(
            key = "someKey",
            bookingCategories = Set()
          ))
      val result: Future[Result] =
        controller.createProject(OrganisationId())(request)

      status(result) must equalTo(FORBIDDEN)
    }

    "badrequest blank key was specified" in new WithTestApplication {

      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: ProjectsControllerMock =
        controllers.ProjectsControllerMock(systemServices,
                                           authConfig,
                                           reactiveMongoApi)

      val request: FakeRequest[CreateProject] = FakeRequest()
        .withBody(
          CreateProject(
            key = "",
            bookingCategories = Set()
          ))
      val result: Future[Result] =
        controller.createProject(controller.organisationId)(request)

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo(
        "expected non-blank String for field 'key'")
    }

    "badrequest creating project with same key and organisation" in new WithTestApplication {

      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: ProjectsControllerMock =
        controllers.ProjectsControllerMock(systemServices,
                                           authConfig,
                                           reactiveMongoApi)

      val request: FakeRequest[CreateProject] = FakeRequest()
        .withBody(
          CreateProject(
            key = controller.project.key,
            bookingCategories = Set()
          ))
      val result: Future[Result] =
        controller.createProject(controller.organisationId)(request)

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo(
        s"Cannot create project with same key ${controller.project.key} in organisation ${controller.organisationId.value}")
    }

    "successful, user assigned to new project as ProjectAdministrator" in new WithTestApplication {

      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: ProjectsControllerMock =
        controllers.ProjectsControllerMock(systemServices,
                                           authConfig,
                                           reactiveMongoApi)
      val newProjectKey: String = "someNewProjectKey"

      val request: FakeRequest[CreateProject] = FakeRequest()
        .withBody(
          CreateProject(
            key = newProjectKey,
            bookingCategories = Set()
          ))
      val result: Future[Result] =
        controller.createProject(controller.organisationId)(request)

      status(result) must equalTo(CREATED)
      val resultingProject: Project = contentAsJson(result).as[Project]
      resultingProject.key === newProjectKey
      resultingProject.organisationReference === controller.organisation.reference

      // verify user gets automatically assigned to this project
      val maybeUser: Option[User] = withDBSession()(implicit dbSession =>
        controller.userRepository.findByUserReference(controller.userReference))
        .awaitResult()
      maybeUser must beSome
      val user: User = maybeUser.get
      val userOrg: Option[UserOrganisation] = user.organisations.find(
        _.organisationReference.id == controller.organisationId)
      userOrg must beSome
      val userProject: Option[UserProject] = userOrg.get.projects.find(
        _.projectReference == resultingProject.reference)
      userProject must beSome
      userProject.get.role === ProjectAdministrator
    }
  }

  "deactivate project" should {
    "forbidden if user is not assigned to organisation" in new WithTestApplication {

      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: ProjectsControllerMock =
        controllers.ProjectsControllerMock(systemServices,
                                           authConfig,
                                           reactiveMongoApi)

      val request: FakeRequest[Unit] = FakeRequest().withBody(())
      val result: Future[Result] =
        controller.deactivateProject(OrganisationId(), controller.project.id)(
          request)

      status(result) must equalTo(FORBIDDEN)
    }
    "badrequest if project id does not exist for given organisation" in new WithTestApplication {

      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: ProjectsControllerMock =
        controllers.ProjectsControllerMock(systemServices,
                                           authConfig,
                                           reactiveMongoApi,
                                           organisationRole =
                                             OrganisationAdministrator)
      val newProjectId: ProjectId = ProjectId()

      val request: FakeRequest[Unit] = FakeRequest().withBody(())
      val result: Future[Result] =
        controller.deactivateProject(controller.organisationId, newProjectId)(
          request)

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo(
        s"Project ${newProjectId.value} does not exist in organisation ${controller.organisation.key}")
    }

    "successful, project unassigned from all users" in new WithTestApplication {

      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: ProjectsControllerMock =
        controllers.ProjectsControllerMock(systemServices,
                                           authConfig,
                                           reactiveMongoApi)
      val request: FakeRequest[Unit] = FakeRequest().withBody(())
      val result: Future[Result] =
        controller.deactivateProject(controller.organisationId,
                                     controller.project.id)(request)

      status(result) must equalTo(OK)

      // verify user gets unassigned from project
      val maybeUser: Option[User] = withDBSession()(implicit dbSession =>
        controller.userRepository.findByUserReference(controller.userReference))
        .awaitResult()
      maybeUser must beSome
      val user: User = maybeUser.get
      val userOrg: Option[UserOrganisation] = user.organisations.find(
        _.organisationReference.id == controller.organisationId)
      userOrg must beSome
      val userProject: Option[UserProject] = userOrg.get.projects.find(
        _.projectReference == controller.project.reference)
      userProject must beNone
    }
  }

  "invite user to project" should {
    "forbidden if user is not assigned to organisation" in new WithTestApplication {

      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: ProjectsControllerMock =
        controllers.ProjectsControllerMock(systemServices,
                                           authConfig,
                                           reactiveMongoApi)

      val request: FakeRequest[UserToProjectAssignment] =
        FakeRequest().withBody(
          UserToProjectAssignment(email = "someNewEmail@test.com",
                                  role = ProjectMember))
      val result: Future[Result] =
        controller.inviteUser(OrganisationId(), controller.project.id)(request)

      status(result) must equalTo(FORBIDDEN)
    }

    "forbidden if user is OrganisationMember and not assigned to project as ProjectAdministrator" in new WithTestApplication {

      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: ProjectsControllerMock =
        controllers.ProjectsControllerMock(systemServices,
                                           authConfig,
                                           reactiveMongoApi,
                                           organisationRole =
                                             OrganisationMember,
                                           projectRole = ProjectMember)

      val request: FakeRequest[UserToProjectAssignment] =
        FakeRequest().withBody(
          UserToProjectAssignment(email = "someNewEmail@test.com",
                                  role = ProjectMember))
      val result: Future[Result] =
        controller.inviteUser(controller.organisationId, controller.project.id)(
          request)

      status(result) must equalTo(FORBIDDEN)
    }

    "badrequest if incorrect email was provided" in new WithTestApplication {

      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: ProjectsControllerMock =
        controllers.ProjectsControllerMock(systemServices,
                                           authConfig,
                                           reactiveMongoApi)

      val request: FakeRequest[UserToProjectAssignment] =
        FakeRequest().withBody(
          UserToProjectAssignment(email = "noEmail", role = ProjectMember))
      val result: Future[Result] =
        controller.inviteUser(controller.organisationId, controller.project.id)(
          request)

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo(
        s"Not a valid email address 'noEmail'")
    }

    "badrequest if project does not exist" in new WithTestApplication {

      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: ProjectsControllerMock =
        controllers.ProjectsControllerMock(systemServices,
                                           authConfig,
                                           reactiveMongoApi)
      val email = "newUserEmail@test.com"

      // delete project
      withDBSession()(implicit dbSession =>
        controller.projectRepository.removeById(controller.project.id))
        .awaitResult()

      val request: FakeRequest[UserToProjectAssignment] =
        FakeRequest().withBody(
          UserToProjectAssignment(email = email, role = ProjectMember))
      val result: Future[Result] =
        controller.inviteUser(controller.organisationId, controller.project.id)(
          request)

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo(
        s"Project ${controller.project.id.value} does not exist in organisation ${controller.organisation.key}")
    }

    "badrequest if project is inactive" in new WithTestApplication {

      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: ProjectsControllerMock =
        controllers.ProjectsControllerMock(systemServices,
                                           authConfig,
                                           reactiveMongoApi,
                                           projectActive = false)
      val email = "newUserEmail@test.com"

      val request: FakeRequest[UserToProjectAssignment] =
        FakeRequest().withBody(
          UserToProjectAssignment(email = email, role = ProjectMember))
      val result: Future[Result] =
        controller.inviteUser(controller.organisationId, controller.project.id)(
          request)

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo(
        s"Cannot invite to an inactive project ${controller.project.key}")
    }

    "successful if user is OrganisationAdministrator" in new WithTestApplication {

      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: ProjectsControllerMock =
        controllers.ProjectsControllerMock(systemServices,
                                           authConfig,
                                           reactiveMongoApi,
                                           organisationRole =
                                             OrganisationAdministrator,
                                           projectRole = ProjectMember)
      val email = "newUserEmail@test.com"
      val request: FakeRequest[UserToProjectAssignment] =
        FakeRequest().withBody(
          UserToProjectAssignment(email = email, role = ProjectMember))
      val result: Future[Result] =
        controller.inviteUser(controller.organisationId, controller.project.id)(
          request)

      status(result) must equalTo(CREATED)
      val invitationResult: InvitationResult =
        contentAsJson(result).as[InvitationResult]
      invitationResult.email === email
    }

    "successful if user is ProjectAdministrator, invitation created if invited user is not part of the same org" in new WithTestApplication {

      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: ProjectsControllerMock =
        controllers.ProjectsControllerMock(systemServices,
                                           authConfig,
                                           reactiveMongoApi)
      val email = "newUserEmail@test.com"
      val request: FakeRequest[UserToProjectAssignment] =
        FakeRequest().withBody(
          UserToProjectAssignment(email = email, role = ProjectMember))
      val result: Future[Result] =
        controller.inviteUser(controller.organisationId, controller.project.id)(
          request)

      status(result) must equalTo(CREATED)
      val invitationResult: InvitationResult =
        contentAsJson(result).as[InvitationResult]
      invitationResult.email === email
      invitationResult.invitationLinkId !== None
    }

    "successful if user is ProjectAdministrator, user directly assigned to project" in new WithTestApplication {

      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: ProjectsControllerMock =
        controllers.ProjectsControllerMock(systemServices,
                                           authConfig,
                                           reactiveMongoApi)

      val email = "ivnitedUser@test.com"
      val userOrganisation: UserOrganisation = UserOrganisation(
        organisationReference = controller.organisation.reference,
        `private` = controller.organisation.`private`,
        role = OrganisationMember,
        plannedWorkingHours = WorkingHours(),
        projects = Seq()
      )
      val user: User = User(
        UserId(),
        "anotherUser",
        email = email,
        password = BCrypt.hashpw("somePassword", BCrypt.gensalt()),
        firstName = "test",
        lastName = "user",
        active = true,
        role = Administrator,
        organisations = Seq(userOrganisation),
        settings = None
      )

      withDBSession()(implicit dbSession =>
        controller.userRepository.upsert(user)).awaitResult()

      val request: FakeRequest[UserToProjectAssignment] =
        FakeRequest().withBody(
          UserToProjectAssignment(email = email, role = ProjectMember))
      val result: Future[Result] =
        controller.inviteUser(controller.organisationId, controller.project.id)(
          request)

      status(result) must equalTo(CREATED)
      val invitationResult: InvitationResult =
        contentAsJson(result).as[InvitationResult]
      invitationResult.email === email
      invitationResult.invitationLinkId === None
    }
  }

  "remove other user from project" should {
    "forbidden if user is not assigned to organisation" in new WithTestApplication {

      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: ProjectsControllerMock =
        controllers.ProjectsControllerMock(systemServices,
                                           authConfig,
                                           reactiveMongoApi)

      val request: FakeRequest[Unit] = FakeRequest().withBody(())
      val result: Future[Result] =
        controller.unassignUser(OrganisationId(),
                                controller.project.id,
                                UserId())(request)

      status(result) must equalTo(FORBIDDEN)
    }

    "forbidden if user is OrganisationMember and not assigned as Administrator to project" in new WithTestApplication {

      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: ProjectsControllerMock =
        controllers.ProjectsControllerMock(systemServices,
                                           authConfig,
                                           reactiveMongoApi,
                                           organisationRole =
                                             OrganisationMember,
                                           projectRole = ProjectMember)

      val request: FakeRequest[Unit] = FakeRequest().withBody(())
      val result: Future[Result] =
        controller.unassignUser(controller.organisationId,
                                controller.project.id,
                                UserId())(request)

      status(result) must equalTo(FORBIDDEN)
    }

    def successfulUnassignUser(controller: ProjectsControllerMock) = {
      // initialize second user
      val userProject = UserProject(
        sharedByOrganisationReference = None,
        projectReference = controller.project.reference,
        role = ProjectMember
      )
      val userOrganisation = UserOrganisation(
        organisationReference = controller.organisation.reference,
        `private` = controller.organisation.`private`,
        role = OrganisationMember,
        plannedWorkingHours = WorkingHours(),
        projects = Seq(userProject)
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
        organisations = Seq(userOrganisation),
        settings = None
      )

      withDBSession()(implicit dbSession =>
        controller.userRepository.upsert(user2)).awaitResult()

      val request: FakeRequest[Unit] = FakeRequest().withBody(())
      val result: Future[Result] =
        controller.unassignUser(controller.organisationId,
                                controller.project.id,
                                user2.id)(request)

      status(result) must equalTo(OK)

      val remainingUsers = withDBSession()(implicit dbSession =>
        controller.userRepository.findByProject(controller.project.id))
        .awaitResult()
      remainingUsers should haveSize(1)
    }

    "successful if user is assigned as OrganisatonAdministrator" in new WithTestApplication {

      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: ProjectsControllerMock =
        controllers.ProjectsControllerMock(systemServices,
                                           authConfig,
                                           reactiveMongoApi,
                                           organisationRole =
                                             OrganisationAdministrator,
                                           projectRole = ProjectMember)
      successfulUnassignUser(controller)
    }

    "successful if user is assigned as ProjectAdministrator" in new WithTestApplication {

      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: ProjectsControllerMock =
        controllers.ProjectsControllerMock(systemServices,
                                           authConfig,
                                           reactiveMongoApi,
                                           organisationRole =
                                             OrganisationMember,
                                           projectRole = ProjectAdministrator)
      successfulUnassignUser(controller)
    }
  }

  "remove own user from project" should {
    "forbidden if user is not assigned to organisation" in new WithTestApplication {

      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: ProjectsControllerMock =
        controllers.ProjectsControllerMock(systemServices,
                                           authConfig,
                                           reactiveMongoApi)

      val request: FakeRequest[Unit] = FakeRequest().withBody(())
      val result: Future[Result] =
        controller.unassignMyUser(OrganisationId(), controller.project.id)(
          request)

      status(result) must equalTo(FORBIDDEN)
    }

    "forbidden if user is not assigned to organisation" in new WithTestApplication {

      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: ProjectsControllerMock =
        controllers.ProjectsControllerMock(systemServices,
                                           authConfig,
                                           reactiveMongoApi)

      val request: FakeRequest[Unit] = FakeRequest().withBody(())
      val result: Future[Result] =
        controller.unassignMyUser(OrganisationId(), controller.project.id)(
          request)

      status(result) must equalTo(FORBIDDEN)
    }

    "successful as ProjectMember" in new WithTestApplication {

      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: ProjectsControllerMock =
        controllers.ProjectsControllerMock(systemServices,
                                           authConfig,
                                           reactiveMongoApi,
                                           projectRole = ProjectMember)

      // initialize second user to be able to remove ourself
      val userProject2: UserProject = UserProject(
        sharedByOrganisationReference = None,
        projectReference = controller.project.reference,
        role = ProjectAdministrator
      )
      val userOrganisation2 = UserOrganisation(
        organisationReference = controller.organisation.reference,
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
        controller.unassignMyUser(controller.organisationId,
                                  controller.project.id)(request)

      status(result) must equalTo(OK)
      val remainingUsers = withDBSession()(implicit dbSession =>
        controller.userRepository.findByProject(controller.project.id))
        .awaitResult()
      remainingUsers should haveSize(1)
    }
  }

  "update project" should {
    "forbidden if user is not assigned to organisation" in new WithTestApplication {

      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: ProjectsControllerMock =
        controllers.ProjectsControllerMock(systemServices,
                                           authConfig,
                                           reactiveMongoApi)

      val request: FakeRequest[UpdateProject] = FakeRequest().withBody(
        UpdateProject(key = Some("newKey"), bookingCategories = None))
      val result: Future[Result] =
        controller.updateProject(OrganisationId(), controller.project.id)(
          request)

      status(result) must equalTo(FORBIDDEN)
    }

    "badrequest no update was provided" in new WithTestApplication {

      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: ProjectsControllerMock =
        controllers.ProjectsControllerMock(systemServices,
                                           authConfig,
                                           reactiveMongoApi)

      val request: FakeRequest[UpdateProject] = FakeRequest().withBody(
        UpdateProject(key = None, bookingCategories = None))
      val result: Future[Result] =
        controller.updateProject(controller.organisationId,
                                 controller.project.id)(request)

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo(
        s"cannot update project ${controller.project.id.value} in organisation ${controller.organisationId.value}, at least one field must be specified")
    }

    "badrequest update with empty key was provided" in new WithTestApplication {

      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: ProjectsControllerMock =
        controllers.ProjectsControllerMock(systemServices,
                                           authConfig,
                                           reactiveMongoApi)

      val request: FakeRequest[UpdateProject] = FakeRequest().withBody(
        UpdateProject(key = Some(""), bookingCategories = None))
      val result: Future[Result] =
        controller.updateProject(controller.organisationId,
                                 controller.project.id)(request)

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo(
        s"expected non-blank String for field 'key'")
    }

    "badrequest if duplicate project key was provided" in new WithTestApplication {

      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: ProjectsControllerMock =
        controllers.ProjectsControllerMock(systemServices,
                                           authConfig,
                                           reactiveMongoApi)

      val project2Key: String = "project2"

      // create second project in same organisation
      withDBSession()(implicit dbSession =>
        controller.projectRepository.upsert(Project(
          id = ProjectId(),
          key = project2Key,
          organisationReference = controller.organisation.reference,
          bookingCategories = Set(),
          active = true,
          createdBy = controller.userReference,
          deactivatedBy = None
        ))).awaitResult()

      val request: FakeRequest[UpdateProject] = FakeRequest().withBody(
        UpdateProject(key = Some(project2Key), bookingCategories = None))
      val result: Future[Result] =
        controller.updateProject(controller.organisationId,
                                 controller.project.id)(request)

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo(
        s"Cannot update project with duplicate key $project2Key in organisation ${controller.organisationId.value}")
    }

    "successful updated key in all references of main-entities" in new WithTestApplication {

      implicit val executionContext: ExecutionContext = inject[ExecutionContext]
      val systemServices: SystemServices              = inject[SystemServices]
      val authConfig: AuthConfig                      = inject[AuthConfig]
      val controller: ProjectsControllerMock =
        controllers.ProjectsControllerMock(systemServices,
                                           authConfig,
                                           reactiveMongoApi)
      val newKey       = "newProjectKey"
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
            organisationReference = controller.organisation.reference,
            `private` = false,
            role = OrganisationMember,
            plannedWorkingHours = WorkingHours(),
            projects = Seq(
              UserProject(sharedByOrganisationReference = None,
                          projectReference =
                            EntityReference(ProjectId(), "anotherProject"),
                          role = ProjectMember),
              UserProject(sharedByOrganisationReference = None,
                          projectReference = controller.project.reference,
                          role = ProjectMember),
            )
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

      val invitation: JoinProjectInvitation = JoinProjectInvitation(
        id = invitationId,
        invitedEmail = "someEmail",
        createDate = DateTime.now(),
        createdBy = controller.userReference,
        expiration = DateTime.now().plusDays(1),
        sharedByOrganisationReference = controller.organisation.reference,
        projectReference = controller.project.reference,
        role = ProjectMember,
        outcome = None
      )

      withDBSession() { implicit dbSession =>
        // create invitation
        for {
          _ <- controller.userRepository.upsert(anotherUser)
          _ <- controller.invitationRepository.upsert(invitation)
        } yield ()
      }.awaitResult()

      val request: FakeRequest[UpdateProject] = FakeRequest().withBody(
        UpdateProject(key = Some(newKey), bookingCategories = None))
      val result: Future[Result] =
        controller.updateProject(controller.organisationId,
                                 controller.project.id)(request)

      status(result) must equalTo(OK)
      val updatedProject = contentAsJson(result).as[Project]

      updatedProject.key === newKey

      // verify references where updated as well
      val updatedInvitation = withDBSession()(implicit dbSession =>
        controller.invitationRepository.findById(invitationId)).awaitResult()
      updatedInvitation must beSome
      updatedInvitation.get
        .asInstanceOf[JoinProjectInvitation]
        .projectReference
        .key === newKey

      val user = withDBSession()(implicit dbSession =>
        controller.userRepository.findById(controller.userId)).awaitResult()
      user must beSome

      user.get.organisations
        .flatMap(_.projects)
        .find(_.projectReference.id == controller.project.id)
        .map(_.projectReference.key) === Some(newKey)

      // check project was changed in second user
      val user2 = withDBSession()(implicit dbSession =>
        controller.userRepository.findById(anotherUser.id)).awaitResult()
      user2 must beSome
      user2.get.organisations
        .flatMap(_.projects)
        .find(_.projectReference.id == controller.project.id)
        .map(_.projectReference.key) === Some(newKey)

      user2.get.organisations
        .flatMap(_.projects)
        .filter(_.projectReference.id != controller.project.id)
        .map(_.projectReference.key) must not contain newKey
    }
  }
}
