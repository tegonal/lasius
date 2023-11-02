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

package util

import akka.stream.testkit.NoMaterializer
import org.pac4j.core.config.Config
import org.pac4j.core.context.session.SessionStore
import org.pac4j.play.scala.DefaultSecurityComponents
import org.specs2.mock.Mockito.mock
import play.api.mvc.BodyParsers
import play.api.test.Helpers

object SecurityComponents {
  def stubSecurityComponents(): DefaultSecurityComponents =
    DefaultSecurityComponents(
      sessionStore = mock[SessionStore],
      config = mock[Config],
      parser =
        new BodyParsers.Default(Helpers.stubPlayBodyParsers(NoMaterializer)),
      components = Helpers.stubControllerComponents()
    )
}
