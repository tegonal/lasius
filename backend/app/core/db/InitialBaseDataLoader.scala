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

package core.db

import core.{DBSession, DBSupport}
import models.UserId.UserReference
import models._
import org.mindrot.jbcrypt.BCrypt
import play.api.Logging
import play.modules.reactivemongo.ReactiveMongoApi
import repositories._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

/*
 * Initialize database with a single user assign to a single organisation
 */
class InitialBaseDataLoader @Inject() (
    val reactiveMongoApi: ReactiveMongoApi,
    OAuthUserRepository: OAuthUserRepository,
    userRepository: UserRepository,
    projectRepository: ProjectRepository,
    organisationRepository: OrganisationRepository)(implicit
    executionContext: ExecutionContext)
    extends Logging
    with DBSupport
    with InitialDataLoader {

  // get's overridden b the withinTransaction call
  override val supportTransaction = true

  private val initialUserKey: String =
    sys.env.getOrElse("LASIUS_INITIAL_USER_KEY", "admin")
  private val initialUserEmail: String =
    sys.env.getOrElse("LASIUS_INITIAL_USER_EMAIL", "admin@lasius.ch")
  private val initialUserPasswordHash: String = BCrypt.hashpw(
    sys.env.getOrElse("LASIUS_INITIAL_USER_PASSWORD", "admin"),
    BCrypt.gensalt())

  override def initializeData(supportTransaction: Boolean)(implicit
      userReference: UserReference): Future[Unit] = {
    logger.debug("Initialize base data...")
    withDBSession(withTransaction = supportTransaction) { implicit dbSession =>
      for {
        org     <- initializeOrganisation()
        project <- initializeProject(org)
        _       <- initializeUser(org, project)
      } yield ()
    }
  }

  private def initializeOrganisation()(implicit
      dbSession: DBSession,
      userReference: UserReference): Future[Organisation] = {
    val org =
      Organisation(OrganisationId(),
                   "MyOrg",
                   `private` = true,
                   active = true,
                   userReference,
                   None)

    organisationRepository.upsert(org).map(_ => org)
  }

  private def initializeProject(org: Organisation)(implicit
      dbSession: DBSession,
      userReference: UserReference): Future[Project] = {
    val project = Project(
      ProjectId(),
      "MyProject",
      org.getReference(),
      Set(
        TagGroup(TagId("Development"),
                 relatedTags = Seq(SimpleTag(TagId("Billable")))),
        TagGroup(TagId("Planning"),
                 relatedTags = Seq(SimpleTag(TagId("Billable")),
                                   SimpleTag(TagId("Admin")))),
        TagGroup(TagId("Administration"),
                 relatedTags = Seq(SimpleTag(TagId("Non-Billable")),
                                   SimpleTag(TagId("Admin"))))
      ),
      active = true,
      userReference,
      None
    )
    projectRepository
      .upsert(project)
      .map(_ => project)
  }

  private def initializeUser(org: Organisation, project: Project)(implicit
      dbSession: DBSession,
      userReference: UserReference): Future[Unit] = {

    val userOrg = UserOrganisation(
      org.getReference(),
      `private` = org.`private`,
      OrganisationAdministrator,
      WorkingHours(),
      Seq(UserProject(None, project.getReference(), ProjectAdministrator))
    )

    OAuthUserRepository.upsert(
      OAuthUser(
        OAuthUserId(),
        email = initialUserEmail,
        password = initialUserPasswordHash,
        firstName = Some("Admin"),
        lastName = Some("Admin"),
        active = true,
      ))

    userRepository.upsert(
      User(
        id = UserId(),
        key = initialUserKey,
        email = initialUserEmail,
        firstName = "Admin",
        lastName = "Admin",
        active = true,
        role = FreeUser,
        organisations = Seq(userOrg),
        settings = None
      ))

  }
}
