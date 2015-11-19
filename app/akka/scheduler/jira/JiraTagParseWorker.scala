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

object JiraTagParseWorker {
  def props(config: JiraConfiguration, auth:JiraAuthentication, projectId:String): Props = Props(classOf[JiraTagParseWorker], config, auth, projectId)
  
  case object StartParsing 
  case object Parse
}

class MyJiraApiServiceImpl(override val config:JiraConfiguration) extends JiraApiServiceImpl {
  override val ws: WSClient = WS.client
}

class JiraTagParseWorker(config:JiraConfiguration, implicit val auth:JiraAuthentication, projectId:String) extends Actor with ActorLogging {
  import JiraTagParseWorker._
  
  var cancellable: Option[Cancellable] = None
  var lastIssueKey:Option[String] = None
  val jiraApiService = new MyJiraApiServiceImpl(config)
  
  val receive: Receive = {
    case StartParsing =>
      cancellable = Some(context.system.scheduler.schedule(0 milliseconds, 10000 milliseconds, self, Parse))
      context become parsing
  }
  
  val parsing: Receive = {
    case Parse =>      
      issues(0, 1).map(_.headOption map { issue =>
        log.debug(s"Latest issue:${issue.key}")
        lastIssueKey match {
          case Some(issue.key) =>
            //Nothing to do, still same last issue key
          case _ =>
            loadIssues(0) map {issues =>
              lastIssueKey = issues.headOption.map(_.key)
              issues.map {issues => 
                log.debug(s"Loaded issue:${issue.key}")                
              }
              //TODO: notify about issues
            }
        }
      })
  }
  
  def loadIssues(offset:Int):Future[Seq[JiraIssue]] = {
    val maxSize = 50
    issues(offset, maxSize).flatMap{issues =>      
      if (issues.size == maxSize) {
        //still more to fetch
        if (lastIssueKey.isEmpty || issues.filter(_.key == lastIssueKey.get).isEmpty) {
          //still not found
          loadIssues(offset + maxSize).map(nextIssues => issues ++ nextIssues)
        }
        else {
          Future.successful(issues)
        }
      } else {
        Future.successful(issues)
      }      
    }
  }
  
  def issues(offset:Int, max:Int) = {
    jiraApiService.findIssues(s"project='$projectId' ORDER BY created DESC", offset, max)
  }
  
  override def postStop() = {
    cancellable.map(c => c.cancel)
    cancellable = None
    super.postStop()
  }
}