/*   __                          __                                          *\
*   / /____ ___ ____  ___  ___ _/ /       lasius                      *
*  / __/ -_) _ `/ _ \/ _ \/ _ `/ /        contributed by tegonal              *
*  \__/\__/\_, /\___/_//_/\_,_/_/         http://tegonal.com/                 *
*         /___/                                                               *
*                                                                             *
* This program is free software: you can redistribute it and/or modify it     *
* under the terms of the GNU General Public License as published by    *
* the Free Software Foundation, either version 3 of the License,              *
* or (at your option) any later version.                                      *
*                                                                             *
* This program is distributed in the hope that it will be useful, but         *
* WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY  *
* or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for *
* more details.                                                               *
*                                                                             *
* You should have received a copy of the GNU General Public License along     *
* with this program. If not, see http://www.gnu.org/licenses/                 *
*                                                                             *
\*                                                                           */
package domain

import akka.testkit.TestProbe
import akka.PersistentActorTestScope
import domain.AggregateRoot._
import domain.LoginStateAggregate._
import models.{UserId, UserLoggedIn, UserLoggedOut}
import org.specs2.mutable.Specification

class LoginStateAggregateSpec extends Specification {

  "LoginStateggregate" should {
    "user login" in new PersistentActorTestScope {

      val probe = TestProbe()
      val actorRef = system.actorOf(LoginStateAggregate.props)

      val userId = UserId("user1")
      val userId2 = UserId("user2")

      actorRef ! Initialize(LoggedInState(Seq()))
      probe.send(actorRef, UserLoggedIn(userId))
      probe.expectMsg(LoggedInState(Seq(userId)))
    }

    "user logout" in new PersistentActorTestScope {
      val probe = TestProbe()
      val actorRef = system.actorOf(LoginStateAggregate.props)

      val userId = UserId("user1")
      val userId2 = UserId("user2")

      actorRef ! Initialize(LoggedInState(Seq(userId, userId2)))
      probe.send(actorRef, UserLoggedOut(userId))
      probe.expectMsg(LoggedInState(Seq(userId2)))
    }
  }
}
