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
import akka.scheduler.jira.JiraTagParseScheduler
import scala.concurrent.ExecutionContext.Implicits.global
import services.JiraConfiguration
import services.OAuthAuthentication
import akka.scheduler.jira.JiraTagParseScheduler.StartScheduler

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
    
  val receive: Receive = {
    case Startup => 
      initialize
    case Shutdown => 
      jiraTagParseScheduler ! JiraTagParseScheduler.StopAllSchedulers
  }
  
  def initialize = {
    initializeJiraPlugin
  }
  
  def initializeJiraPlugin = {
    //start jira parse scheduler for every project attached to a jira configuration
    jiraConfigRepository.getJiraConfigurations() map { _.map { config =>
        val jiraConfig = JiraConfiguration(config.baseUrl.toString)
        val auth = OAuthAuthentication(config.consumerKey, config.privateKey, config.accessToken)
        
        config.projects.map { proj =>
          log.debug(s"Start parsing for the following configuration:$jiraConfig - $proj")
          jiraTagParseScheduler ! StartScheduler(jiraConfig, auth, proj.projectId, proj.jiraProjectKey)
        }
      }
    }
  }
}