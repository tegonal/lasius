package akka.scheduler.jira

import akka.actor.Actor
import akka.actor.ActorLogging
import services.JiraAuthentication
import akka.actor.Props
import akka.actor.Cancellable
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import services.JiraApiServiceImpl
import play.api.libs.ws.WSClient
import play.api.libs.ws.WS
import play.api.Play.current
import services.JiraConfiguration
import scala.concurrent.Future
import models.JiraIssue
import models.ProjectId
import models.JiraSettings
import models.ProjectSettings

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
        
        //handle new parsed issue keys
      }.andThen{
        case s =>
          //restart timer
          log.debug(s"andThen:restart time $s")
          cancellable = Some(context.system.scheduler.scheduleOnce(10000 milliseconds, self, Parse))          
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
    log.error(s"Parse issues projectId=${projectId.value}, project=${projectSettings.jiraProjectKey}, offset:$offset, max:$max")
    val query = projectSettings.jql.getOrElse(defaultJql)
    jiraApiService.findIssues(query, Some(offset), Some(max))
  }
  
  override def postStop() = {
    cancellable.map(c => c.cancel)
    cancellable = None
    super.postStop()
  }
}