package akka.scheduler.jira

import akka.actor.Actor
import akka.actor.ActorLogging
import services.JiraAuthentication
import java.util.UUID
import akka.actor.Cancellable
import akka.actor.ActorRef
import akka.actor.PoisonPill
import services.JiraConfiguration
import akka.scheduler.jira.JiraTagParseWorker.StartParsing
import akka.actor.OneForOneStrategy
import scala.concurrent.duration._
import akka.actor.SupervisorStrategy._
import akka.actor.Props
import models.ProjectId

object JiraTagParseScheduler {
  def props = Props(classOf[JiraTagParseScheduler])
  
  case class StartScheduler(config:JiraConfiguration, auth: JiraAuthentication, projectId: ProjectId, jiraProjectKey: String)
  case class StopScheduler(uuid:UUID)
  case class StopAllSchedulers()
  case class SchedulerStarted(uuid: UUID)
}

class JiraTagParseScheduler extends Actor with ActorLogging {
  import JiraTagParseScheduler._
  var workers: Map[UUID, ActorRef] = Map()
  
  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
      case _                => Restart
    }
  
  val receive: Receive = {
    case StartScheduler(config, auth, projectId, projectKey) =>
      log.error(s"StartScheduler: $config, $auth, $projectId, $projectKey")
      val uuid = UUID.randomUUID
      val ref = context.actorOf(JiraTagParseWorker.props(config, auth, projectId, projectKey))
      workers += uuid -> ref
      ref ! StartParsing
      sender ! SchedulerStarted(uuid)
    case StopScheduler(uuid) => 
      log.error(s"StopScheduler: $uuid")
      workers = workers.get(uuid).map{worker =>
        log.debug(s"Stopping worker with uuid:$uuid")
        worker ! PoisonPill
        workers - uuid
      }.getOrElse(workers)
       
    case StopAllSchedulers => 
      log.error("Stopping all workers")
      workers.map{case (_, worker) => worker ! PoisonPill}
  }
}