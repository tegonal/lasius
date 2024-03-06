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

import akka.actor.ActorSystem
import mongo.{EmbedMongo, MongoDb}
import play.api.inject
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.{Injecting, PlaySpecification, WithApplication}

trait TestApplication extends EmbedMongo {
  self: PlaySpecification =>

  protected def appConfiguration: Map[String, Any] =
    mongoDb.mongoConfiguration

  abstract class WithTestApplication
      extends WithApplication(
        app = GuiceApplicationBuilder()
          .configure(appConfiguration)
          .overrides(inject
                       .bind[SystemServices]
                       .toProvider[MockServicesProvider],
                     inject
                       .bind[ActorSystem]
                       .toProvider[PlayAwareActorSystemProvider])
          .build())
      with Injecting
}
