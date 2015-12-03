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
import java.util.UUID
import services.JiraConfiguration
import actors.scheduler.jira.JiraTagParseWorker.StartParsing
import akka.actor.OneForOneStrategy
import scala.concurrent.duration._
import akka.actor.SupervisorStrategy._
import akka.actor.Props
import models._

object JiraTagParseScheduler {
  def props = Props(classOf[JiraTagParseScheduler])
  
  case class StartScheduler(config:JiraConfiguration, settings:JiraSettings, projectSettings: ProjectSettings, auth: JiraAuthentication, projectId: ProjectId)
  case class StopScheduler(uuid:UUID)
  case object StopAllSchedulers
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
    case StartScheduler(config, settings, projectSettings, auth, projectId) =>
      log.error(s"StartScheduler: $config, $auth, $projectId, ${projectSettings.jiraProjectKey}")
      val uuid = UUID.randomUUID
      val ref = context.actorOf(JiraTagParseWorker.props(config, settings, projectSettings, auth, projectId))
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