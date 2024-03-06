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

package akka

import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestKitBase}
import com.typesafe.config.ConfigFactory
import core.TestApplication
import org.specs2.matcher.Scope
import play.api.test.PlaySpecification

import java.util
import scala.jdk.CollectionConverters.{IterableHasAsJava, MapHasAsJava}

class ActorTestScope
    extends TestKit(ActorSystem("lasius-test-actor-system"))
    with Scope

trait PersistentActorTestScope extends TestApplication {
  self: PlaySpecification =>

  abstract class WithPersistentActorTestScope
      extends WithTestApplication
      with TestKitBase {

    implicit val system: ActorSystem = ActorSystem(
      name = "lasius-test-actor-system",
      config = ConfigFactory
        .parseMap(appConfiguration.asJavaNested)
        .withFallback(ConfigFactory.load())
    )
  }

  implicit class MyMap(map: Map[String, Any]) {
    def asJavaNested: util.Map[String, Any] =
      map.view
        .mapValues {
          case i: Iterable[_] => i.asJava
          case x              => x
        }
        .toMap
        .asJava
  }
}
