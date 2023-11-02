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

import core.{DBSession, DBSupport, SystemServices}
import domain.UserTimeBookingAggregate.AddBookingCommand
import models.UserId.UserReference
import models._
import org.joda.time.{DateTime, Interval}
import org.mindrot.jbcrypt.BCrypt
import play.api.Logging
import play.modules.reactivemongo.ReactiveMongoApi
import repositories._

import javax.inject.Inject
import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class InitialDemoDataLoader @Inject() (
    val reactiveMongoApi: ReactiveMongoApi,
    oauthUserRepository: OAuthUserRepository,
    userRepository: UserRepository,
    projectRepository: ProjectRepository,
    organisationRepository: OrganisationRepository,
    systemServices: SystemServices)(implicit executionContext: ExecutionContext)
    extends Logging
    with DBSupport
    with InitialDataLoader {

  private val randomPhraseList: List[SimpleTag] =
    """   You Can't Teach an Old Dog New Tricks
          Shot In the Dark
          Jack of All Trades Master of None
          It's Not Brain Surgery
          A Bite at the Cherry
          Scot-free
          Let Her Rip
          Keep Your Shirt On
          Beating Around the Bush
          Mouth-watering
          Jig Is Up
          A Chip on Your Shoulder
          Jaws of Death
          Elvis Has Left The Building
          High And Dry
          Cry Wolf
          I Smell a Rat
          A Cold Day in July
          Needle In a Haystack
          Love Birds
          Go Out On a Limb
          Ride Him, Cowboy!
          What Am I, Chopped Liver?
          On Cloud Nine
          Drive Me Nuts
          Top Drawer
          Mountain Out of a Molehill
          A Cat Nap
          Wake Up Call
          Elephant in the Room
          Back To the Drawing Board
          A Guinea Pig
          Swinging For the Fences
          If You Can't Stand the Heat, Get Out of the Kitchen
          Poke Fun At
          Give a Man a Fish
          An Arm and a Leg
          Back to Square One
          Fish Out Of Water
          Drawing a Blank
          Greased Lightning
          A Leg Up
          A Fool and His Money Are Soon Parted
          Read 'Em and Weep
          A Little from Column A, a Little from Column B
          Cry Over Spilt Milk
          Go For Broke
          In a Pickle
          Ugly Duckling
          Long In The Tooth"""
      .split("\n")
      .map(w => SimpleTag(TagId(w.trim)))
      .toList

  // get's overridden b the withinTransaction call
  override val supportTransaction = true

  private val user1Key: String = sys.env.getOrElse("DEMO_USER1_KEY", "demo1")
  private val user1Email: String =
    sys.env.getOrElse("DEMO_USER1_EMAIL", "demo1@lasius.ch")
  private val user1PasswordHash: String = BCrypt.hashpw(
    sys.env.getOrElse("DEMO_USER1_PASSWORD", "demo"),
    BCrypt.gensalt())

  private val user2Key: String = sys.env.getOrElse("DEMO_USER2_KEY", "demo2")
  private val user2Email: String =
    sys.env.getOrElse("DEMO_USER2_EMAIL", "demo2@lasius.ch")
  private val user2PasswordHash: String = BCrypt.hashpw(
    sys.env.getOrElse("DEMO_USER2_PASSWORD", "demo"),
    BCrypt.gensalt())

  override def initializeData(supportTransaction: Boolean)(implicit
      userReference: UserReference): Future[Unit] = {
    logger.debug("Initialize base data...")
    withDBSession(withTransaction = supportTransaction) { implicit dbSession =>
      for {
        (user1org, user2org, org) <- initializeOrganisations()
        projects                  <- initializeProjects(org)
        users <- initializeUsers(user1org, user2org, org, projects)
        _     <- initializeTimeBookings(org, projects, users)
      } yield ()
    }
  }

  private def initializeOrganisations()(implicit
      dbSession: DBSession,
      userReference: UserReference)
      : Future[(Organisation, Organisation, Organisation)] = {

    val user1org =
      Organisation(OrganisationId(),
                   user1Key,
                   `private` = true,
                   active = true,
                   userReference,
                   None)

    val user2org =
      Organisation(OrganisationId(),
                   user2Key,
                   `private` = true,
                   active = true,
                   userReference,
                   None)

    val org =
      Organisation(OrganisationId(),
                   "DemoOrg",
                   `private` = false,
                   active = true,
                   userReference,
                   None)

    organisationRepository
      .bulkInsert(List(user1org, user2org, org))
      .map(_ => (user1org, user2org, org))
  }

  private def initializeProjects(org: Organisation)(implicit
      dbSession: DBSession,
      userReference: UserReference): Future[Seq[Project]] = {
    val projects = List(
      Project(
        ProjectId(),
        "Lasius",
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
      ),
      Project(
        ProjectId(),
        "Marketing",
        org.getReference(),
        Set(
          TagGroup(TagId("Sales"),
                   relatedTags = Seq(SimpleTag(TagId("Non-Billable")),
                                     SimpleTag(TagId("Customer Contact")))),
          TagGroup(TagId("Cold Aquisition"),
                   relatedTags = Seq(SimpleTag(TagId("Non Billable")),
                                     SimpleTag(TagId("No Customer Contact"))))
        ),
        active = true,
        userReference,
        None
      ),
      Project(
        ProjectId(),
        "KnowHow",
        org.getReference(),
        Set(SimpleTag(TagId("Billable")), SimpleTag(TagId("Non-Billable"))),
        active = true,
        userReference,
        None),
      Project(
        ProjectId(),
        "Others",
        org.getReference(),
        Set(SimpleTag(TagId("Billable")), SimpleTag(TagId("Non-Billable"))),
        active = true,
        userReference,
        None)
    )

    projectRepository.bulkInsert(projects).map(_ => projects)
  }

  private def initializeUsers(user1Org: Organisation,
                              user2Org: Organisation,
                              publicOrg: Organisation,
                              projects: Seq[Project])(implicit
      dbSession: DBSession,
      userReference: UserReference): Future[List[User]] = {

    val oauthUser1 = OAuthUser(
      id = OAuthUserId(),
      email = user1Email,
      password = user1PasswordHash,
      firstName = Some("Demo"),
      lastName = Some("User 1"),
      active = true,
    )

    val user1 = User(
      id = UserId(),
      key = user1Key,
      email = user1Email,
      firstName = "Demo",
      lastName = "User 1",
      active = true,
      role = FreeUser,
      organisations = Seq(
        UserOrganisation(
          user1Org.getReference(),
          `private` = user1Org.`private`,
          OrganisationAdministrator,
          WorkingHours(),
          Seq()
        ),
        UserOrganisation(
          publicOrg.getReference(),
          publicOrg.`private`,
          OrganisationAdministrator,
          WorkingHours(monday = 8, tuesday = 4, wednesday = 2),
          projects
            .filter(p => Seq("Lasius", "KnowHow").contains(p.key))
            .map(p =>
              UserProject(None, p.getReference(), ProjectAdministrator)) ++
            projects
              .filter(p => Seq("Marketing", "Others").contains(p.key))
              .map(p => UserProject(None, p.getReference(), ProjectMember))
        )
      ),
      settings = Some(
        UserSettings(lastSelectedOrganisation = Some(publicOrg.getReference())))
    )

    val oauthUser2 = OAuthUser(
      id = OAuthUserId(),
      email = user2Email,
      password = user2PasswordHash,
      firstName = Some("Demo"),
      lastName = Some("User 2"),
      active = true,
    )
    val user2 = User(
      id = UserId(),
      key = user2Key,
      email = user2Email,
      firstName = "Demo",
      lastName = "User 2",
      active = true,
      role = FreeUser,
      organisations = Seq(
        UserOrganisation(
          user2Org.getReference(),
          `private` = user2Org.`private`,
          OrganisationAdministrator,
          WorkingHours(),
          Seq()
        ),
        UserOrganisation(
          publicOrg.getReference(),
          `private` = publicOrg.`private`,
          OrganisationAdministrator,
          WorkingHours(monday = 8, tuesday = 4, wednesday = 2),
          projects
            .filter(p => Seq("Lasius", "KnowHow").contains(p.key))
            .map(p => UserProject(None, p.getReference(), ProjectMember)) ++
            projects
              .filter(p => Seq("Marketing", "Others").contains(p.key))
              .map(p =>
                UserProject(None, p.getReference(), ProjectAdministrator))
        )
      ),
      settings = Some(
        UserSettings(lastSelectedOrganisation = Some(publicOrg.getReference())))
    )

    val users = List(user1, user2)

    oauthUserRepository.bulkInsert(List(oauthUser1, oauthUser2))
    userRepository.bulkInsert(users).map(_ => users)
  }

  private def initializeTimeBookings(org: Organisation,
                                     projects: Seq[Project],
                                     users: Seq[User])(implicit
      dbSession: DBSession,
      userReference: UserReference): Future[Seq[Seq[Seq[Unit]]]] =
    Future {
      users.map(initializeUserTimeBookings(org, projects, _))
    }

  /** Generate time bookings for a given user for the last 60 days
    */
  private def initializeUserTimeBookings(org: Organisation,
                                         projects: Seq[Project],
                                         user: User)(implicit
      dbSession: DBSession): Seq[Seq[Unit]] = {
    val now           = DateTime.now()
    val orgRef        = org.getReference()
    val random        = new Random
    val userReference = user.getReference()
    (1 to 60).map { dayDiff =>
      val day = now.minusDays(dayDiff)
      generateRandomTimeSlots(day).map { timeSlot =>
        // pick random project
        val project = projects(random.nextInt(projects.length))

        // pick a random tag from the list
        val projectTags = random
          .shuffle(project.bookingCategories)
          .take(random.between(1, project.bookingCategories.size))
        val randomPhrase = random.shuffle(randomPhraseList).head

        systemServices.timeBookingViewService ! AddBookingCommand(
          userReference = userReference,
          organisationReference = orgRef,
          projectReference = project.getReference(),
          tags = projectTags + randomPhrase,
          start = timeSlot._1,
          end = timeSlot._2
        )
      }
    }
  }

  private def generateRandomTimeSlots(
      day: DateTime): Seq[(DateTime, DateTime)] = {
    val rand       = Random
    val startOfDay = day.withHourOfDay(8).plusMinutes(rand.between(-30, 30))
    val endOfDay   = day.withHourOfDay(17).plusMinutes(rand.between(-90, 75))

    val secondsBetween =
      new Interval(startOfDay, endOfDay).toDuration.getMillis / 1000
    val numberOfSplits = rand.between(2, 10)

    generateTimeSlots(startOfDay,
                      endOfDay,
                      numberOfSplits,
                      secondsBetween,
                      Seq())
  }

  @tailrec
  private def generateTimeSlots(
      start: DateTime,
      end: DateTime,
      numberOfSplits: Int,
      rangeSeconds: Long,
      splits: Seq[(DateTime, DateTime)]): Seq[(DateTime, DateTime)] = {
    val randomBreak = Random.nextInt(300)
    val nextStart   = start.plusSeconds(randomBreak)
    if (numberOfSplits > 0) {
      val workingSeconds =
        Random.between(60, (rangeSeconds - randomBreak) / numberOfSplits).toInt
      val nextEnd = nextStart.plusSeconds(workingSeconds)
      generateTimeSlots(start = nextEnd,
                        end = end,
                        numberOfSplits = numberOfSplits - 1,
                        rangeSeconds = rangeSeconds - workingSeconds,
                        splits = splits :+ (nextStart, nextEnd))
    } else if (end.isAfter(nextStart)) {
      splits :+ (nextStart, end)
    } else {
      splits
    }
  }
}
