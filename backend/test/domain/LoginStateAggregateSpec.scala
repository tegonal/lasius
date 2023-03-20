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

package domain

import akka.PersistentActorTestScope
import akka.testkit.TestProbe
import domain.AggregateRoot._
import domain.LoginStateAggregate._
import models.{EntityReference, UserId, UserLoggedInV2, UserLoggedOutV2}
import org.specs2.mutable.Specification

class LoginStateAggregateSpec extends Specification {

  "LoginStateggregate" should {
    "user login" in new PersistentActorTestScope {

      val probe    = TestProbe()
      val actorRef = system.actorOf(LoginStateAggregate.props)

      val userId = EntityReference(UserId(), "user1")

      actorRef ! Initialize(LoggedInState(Set()))
      probe.send(actorRef, UserLoggedInV2(userId))
      probe.expectMsg(LoggedInState(Set(userId)))
    }

    "user logout" in new PersistentActorTestScope {
      val probe    = TestProbe()
      val actorRef = system.actorOf(LoginStateAggregate.props)

      val userId  = EntityReference(UserId(), "user1")
      val userId2 = EntityReference(UserId(), "user2")

      actorRef ! Initialize(LoggedInState(Set(userId, userId2)))
      probe.send(actorRef, UserLoggedOutV2(userId))
      probe.expectMsg(LoggedInState(Set(userId2)))
    }
  }
}
