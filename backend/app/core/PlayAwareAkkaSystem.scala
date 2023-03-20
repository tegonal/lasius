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

import javax.inject.Singleton
import akka.actor.{ActorSystem, _}
import play.api.inject.{BindingKey, Injector}
import play.api.libs.concurrent.ActorSystemProvider
import play.api.{Configuration, Environment}

import javax.inject.{Inject, Provider}
import scala.reflect.ClassTag

@Singleton
class PlayAwareActorSystemProvider @Inject() (environment: Environment,
                                              configuration: Configuration,
                                              injector: Injector)
    extends Provider[ActorSystem] {
  lazy val get: ActorSystem = {
    val actorSystem =
      ActorSystemProvider.start(environment.classLoader, configuration)
    PlayAkkaExtension(actorSystem).initialize(injector)
    actorSystem
  }
}

class PlayAkkaExtensionImpl extends Extension {
  private var injector: Injector = _

  def initialize(injector: Injector): Unit = {
    this.injector = injector
  }

  def instanceOf[T](clazz: Class[T]): T = injector.instanceOf(clazz)

  def instanceOf[T: ClassTag]: T = injector.instanceOf

  def instanceOf[T](key: BindingKey[T]): T = injector.instanceOf(key)
}

object PlayAkkaExtension
    extends ExtensionId[PlayAkkaExtensionImpl]
    with ExtensionIdProvider {
  override def lookup = PlayAkkaExtension

  override def createExtension(system: ExtendedActorSystem) =
    new PlayAkkaExtensionImpl
  override def get(system: ActorSystem): PlayAkkaExtensionImpl =
    super.get(system)
}
