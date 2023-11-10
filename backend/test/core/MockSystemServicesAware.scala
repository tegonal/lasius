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

package core

import actors.LasiusSupervisorActor
import akka.actor.{ActorRef, ActorSystem}
import akka.stream.Materializer
import akka.testkit.TestProbe
import akka.util.Timeout
import models.{ApplicationConfig, EntityReference, Subject, UserId}
import models.UserId.UserReference
import org.pac4j.core.profile.CommonProfile
import org.specs2.mock.Mockito.mock
import play.modules.reactivemongo.ReactiveMongoApi

import javax.inject.{Inject, Provider, Singleton}
import scala.concurrent.duration._
import scala.language.postfixOps

@Singleton
class MockServicesProvider @Inject() (actorSystem: ActorSystem)
    extends Provider[SystemServices] {
  lazy val get: SystemServices =
    new MockServices(actorSystem)
}

class MockServices(actorSystem: ActorSystem) extends SystemServices {

  override val appConfig: ApplicationConfig = ApplicationConfig(
    title = "Lasius",
    instance = "Test",
    lasiusOAuthProviderEnabled = true,
    lasiusOAuthProviderAllowUserRegistration = true
  )
  val supervisor: ActorRef =
    actorSystem.actorOf(LasiusSupervisorActor.props, "lasius-test-supervisor")
  val reactiveMongoApi: ReactiveMongoApi   = mock[ReactiveMongoApi]
  override val supportTransaction: Boolean = false

  implicit val system: ActorSystem        = actorSystem
  override val materializer: Materializer = Materializer.matFromSystem
  val systemUser: UserId                  = UserId()
  override val systemUserReference: UserReference =
    EntityReference(systemUser, "system")
  override val systemUserProfile: CommonProfile = new CommonProfile()
  override val systemSubject: Subject[CommonProfile] =
    Subject(systemUserProfile, systemUserReference)
  implicit val timeout: Timeout = Timeout(5 seconds) // needed for `?` below
  val duration: Duration        = Duration.create(5, SECONDS)
  val timeBookingViewService: ActorRef = TestProbe().ref

  val loginStateAggregate: ActorRef = TestProbe().ref

  val currentUserTimeBookingsViewService: ActorRef  = TestProbe().ref
  val currentOrganisationTimeBookingsView: ActorRef = TestProbe().ref
  val latestUserTimeBookingsViewService: ActorRef   = TestProbe().ref
  val timeBookingStatisticsViewService: ActorRef    = TestProbe().ref
  val tagCache: ActorRef                            = TestProbe().ref
  val pluginHandler: ActorRef                       = TestProbe().ref
  val loginHandler: ActorRef                        = TestProbe().ref

  override def initialize(): Unit = {}
}
