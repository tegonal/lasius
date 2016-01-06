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
import repositories._
import scala.concurrent.ExecutionContext.Implicits.global
import services.JiraConfiguration
import services.OAuthAuthentication
import actors.scheduler.jira.JiraTagParseScheduler
import actors.scheduler.jira.JiraTagParseScheduler._
import play.api.Play
import core.LoginHandler.InitializeUserViews

object PluginHandler {
  def props(): Props = Props(classOf[DefaultPluginHandler])
  
  case object Startup
  case object Shutdown
}

class DefaultPluginHandler extends PluginHandler with MongoBasicRepositoryComponent

trait PluginHandler extends Actor with ActorLogging {
  self: BasicRepositoryComponent =>
    import PluginHandler._
    
    val jiraTagParseScheduler = context.actorOf(JiraTagParseScheduler.props)
    
    log.debug(s"PluginHandler started")
    
  val receive: Receive = {
    case Startup =>
      log.debug(s"PluginHandler startup")
      initialize
    case Shutdown => 
      jiraTagParseScheduler ! JiraTagParseScheduler.StopAllSchedulers
    case e => 
      log.warning(s"Received unknown event:$e")
  }
  
  def initialize = {
    initializeJiraPlugin
    initializeUserViews
  }
  
  private def initializeUserViews = {
    val initializeViews = Play.current.configuration.getBoolean("lasius.persistence.on_startup.initialize_views").getOrElse(false)
    log.debug(s"initializeUserViews:$initializeViews")
    if (initializeViews) {
      userRepository.findAll() map { users =>
        log.debug(s"findAllUsers:$users")
        users.map(user => Global.loginHandler ! InitializeUserViews(user.id))
      }
    }
  }
  
  def initializeJiraPlugin = {
    log.debug(s"PluginHandler initializeJiraPlugin:$jiraConfigRepository")
    //start jira parse scheduler for every project attached to a jira configuration
    jiraConfigRepository.getJiraConfigurations() map { s =>
      log.debug(s"Got jira configs:$s")
      s.map { config =>
      log.debug(s"Start Jira Scheduler for config:$config")
        val jiraConfig = JiraConfiguration(config.baseUrl.toString)
        val auth = OAuthAuthentication(config.auth.consumerKey, config.auth.privateKey, config.auth.accessToken)
        
        config.projects.map { proj =>
          log.debug(s"Start parsing for the following configuration:$jiraConfig - $proj")
          jiraTagParseScheduler ! StartScheduler(jiraConfig, config.settings, proj.settings, auth, proj.projectId)
        }
      }
    }
  }
}