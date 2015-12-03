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
package actors.scheduler.jira

import akka.actor._
import services.JiraAuthentication
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import services.JiraApiServiceImpl
import play.api.libs.ws.WSClient
import play.api.libs.ws.WS
import play.api.Play.current
import services.JiraConfiguration
import scala.concurrent.Future
import models._
import core.Global._
import actors.TagCache.TagsUpdated
import scala.reflect.runtime.universe._

object JiraTagParseWorker {
  def props(config: JiraConfiguration, settings:JiraSettings, projectSettings: ProjectSettings, auth:JiraAuthentication, projectId:ProjectId): Props = Props(classOf[JiraTagParseWorker], config, settings, projectSettings, auth, projectId)
  
  case object StartParsing 
  case object Parse
}

class MyJiraApiServiceImpl(override val config:JiraConfiguration) extends JiraApiServiceImpl {
  override val ws: WSClient = WS.client
}

class JiraTagParseWorker(config:JiraConfiguration, settings:JiraSettings, projectSettings: ProjectSettings, implicit val auth:JiraAuthentication, projectId:ProjectId) extends Actor with ActorLogging {
  import JiraTagParseWorker._
  
  var cancellable: Option[Cancellable] = None
  var lastIssueSize:Option[Int] = None
  val jiraApiService = new MyJiraApiServiceImpl(config)
  val defaultJql = s"project=${projectSettings.jiraProjectKey} and resolution=Unresolved ORDER BY created DESC"
  val defaultTimeout = 60000 //60 seconds
  val maxResults = projectSettings.maxResults.getOrElse(100)
  
  val receive: Receive = {
    case StartParsing =>
      cancellable = Some(context.system.scheduler.scheduleOnce(0 milliseconds, self, Parse))
      context become parsing
  }
  
  val parsing: Receive = {
    case Parse =>
      loadIssues(0, lastIssueSize).map{result =>
        //fetched all results, notify
        lastIssueSize = Some(result.size)
        val keys = result.map(_.key)
        log.debug(s"Parsed keys:$keys")
        
        //assemble jira issuetag
        val tags = result.map(toJiraIssueTag)
        tagCache ! TagsUpdated[JiraIssueTag](projectId, tags)
        
        //handle new parsed issue keys
      }.andThen{
        case s =>
          //restart timer
          log.debug(s"andThen:restart time $s")
          cancellable = Some(context.system.scheduler.scheduleOnce(10000 milliseconds, self, Parse))          
      }
  }
  
  def toJiraIssueTag(issue:JiraIssue):JiraIssueTag = {
    issue.fields.map { fields => 
      JiraIssueTag(TagId(issue.key), config.baseUrl, fields.primary.summary, 
          issue.self, projectSettings.jiraProjectKey, 
          fields.secondary.versions.map(_.map(_.name)),         
          fields.secondary.fixVersions.map(_.map(_.name)), 
          fields.secondary.components.map(_.map(_.name)),
          fields.secondary.labels.map(_.map(_.name)))
    }.getOrElse {
      JiraIssueTag(TagId(issue.key), config.baseUrl, None, 
          issue.self, projectSettings.jiraProjectKey, 
          None,         
          None, 
          None,
          None)
    }
  }

  def loadIssues(offset:Int, max: Option[Int], lastResult:Set[JiraIssue]=Set()):Future[Set[JiraIssue]] = {    
    val newMax = max.getOrElse(maxResults)
    issues(offset, newMax).flatMap{result =>
      val issues = result.issues
      val concat = (lastResult ++ result.issues.toSet).toSet
      log.debug(s"loaded issues: maxResults:${result.maxResults}, fetch count${concat.size}")
      if (result.maxResults >= concat.size) {
          //fetched all results, notify
          Future.successful(concat)
      }
      else {
        val maxNextRun = Math.min(result.maxResults, maxResults)
        loadIssues(newMax, Some(maxNextRun), concat)
      }          
    }
  }
  
  def issues(offset:Int, max:Int) = {
    log.debug(s"Parse issues projectId=${projectId.value}, project=${projectSettings.jiraProjectKey}, offset:$offset, max:$max")
    val query = projectSettings.jql.getOrElse(defaultJql)
    jiraApiService.findIssues(query, Some(offset), Some(max))
  }
  
  override def postStop() = {
    cancellable.map(c => c.cancel)
    cancellable = None
    super.postStop()
  }
}