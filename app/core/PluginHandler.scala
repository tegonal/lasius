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
package core

import akka.actor._
import core.LoginHandler.InitializeUserViews
import repositories._

object PluginHandler {
  def props(): Props = Props(classOf[DefaultPluginHandler])
  
  case object Startup
  case object Shutdown
}

class DefaultPluginHandler
    extends PluginHandler
    with MongoBasicRepositoryComponent
    with DefaultSystemServicesAware

trait PluginHandler extends Actor with ActorLogging with SystemServicesAware  with ConfigAware{
  self: BasicRepositoryComponent =>
  import PluginHandler._

  log.debug(s"PluginHandler started")

  val receive: Receive = {
    case Startup =>
      log.debug(s"PluginHandler startup")
      initialize
    case Shutdown =>
    case e =>
      log.warning(s"Received unknown event:$e")
  }

  def initialize = {
    initializeUserViews
  }

  private def initializeUserViews = {
    val initializeViews = config
      .getBoolean("lasius.persistence.on_startup.initialize_views")
    log.debug(s"initializeUserViews:$initializeViews")
    if (initializeViews) {
      userRepository.findAll() foreach { users =>
        log.debug(s"findAllUsers:$users")
        users.foreach(user => systemServices.loginHandler ! InitializeUserViews(user.id))
      }
    }
  }
}
