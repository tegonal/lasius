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
import models.{EntityReference, Subject, UserId}
import models.UserId.UserReference
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

  val supervisor: ActorRef =
    actorSystem.actorOf(LasiusSupervisorActor.props, "lasius-test-supervisor")
  val reactiveMongoApi: ReactiveMongoApi   = mock[ReactiveMongoApi]
  override val supportTransaction: Boolean = false

  implicit val system: ActorSystem        = actorSystem
  override val materializer: Materializer = Materializer.matFromSystem
  val systemUser: UserId                  = UserId()
  override val systemUserReference: UserReference =
    EntityReference(systemUser, "system")
  override val systemSubject: Subject = Subject("", systemUserReference)
  implicit val timeout: Timeout = Timeout(5 seconds) // needed for `?` below
  val duration: Duration        = Duration.create(5, SECONDS)
  val timeBookingViewServiceProbe: TestProbe = TestProbe()
  val timeBookingViewService: ActorRef       = timeBookingViewServiceProbe.ref

  val loginStateAggregateProbe: TestProbe = TestProbe()
  val loginStateAggregate: ActorRef       = loginStateAggregateProbe.ref

  val currentUserTimeBookingsViewServiceProbe: TestProbe = TestProbe()
  val currentUserTimeBookingsViewService: ActorRef =
    currentUserTimeBookingsViewServiceProbe.ref

  val currentOrganisationTimeBookingsViewProbe: TestProbe = TestProbe()
  val currentOrganisationTimeBookingsView: ActorRef =
    currentOrganisationTimeBookingsViewProbe.ref

  val latestUserTimeBookingsViewServiceProbe: TestProbe = TestProbe()
  val latestUserTimeBookingsViewService: ActorRef =
    latestUserTimeBookingsViewServiceProbe.ref

  val timeBookingStatisticsViewServiceProbe: TestProbe = TestProbe()
  val timeBookingStatisticsViewService: ActorRef =
    timeBookingStatisticsViewServiceProbe.ref

  val tagCacheProbe: TestProbe = TestProbe()
  val tagCache: ActorRef       = tagCacheProbe.ref

  val pluginHandlerProbe: TestProbe = TestProbe()
  val pluginHandler: ActorRef       = pluginHandlerProbe.ref

  val loginHandlerProbe: TestProbe = TestProbe()
  val loginHandler: ActorRef       = loginHandlerProbe.ref

  override def initialize(): Unit = {}
}
