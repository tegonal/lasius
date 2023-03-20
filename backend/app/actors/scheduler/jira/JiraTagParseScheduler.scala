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

package actors.scheduler.jira

import java.util.UUID

import _root_.models.{JiraProjectSettings, JiraSettings, ProjectId}
import actors.scheduler.{ServiceAuthentication, ServiceConfiguration}
import actors.scheduler.jira.JiraTagParseWorker.StartParsing
import akka.actor.SupervisorStrategy._
import akka.actor.{OneForOneStrategy, Props, _}
import core.SystemServices
import play.api.libs.ws.WSClient

import scala.concurrent.duration._
import scala.language.postfixOps

object JiraTagParseScheduler {
  def props(wsClient: WSClient, systemServices: SystemServices): Props =
    Props(classOf[JiraTagParseScheduler], wsClient, systemServices)

  case class StartScheduler(config: ServiceConfiguration,
                            settings: JiraSettings,
                            projectSettings: JiraProjectSettings,
                            auth: ServiceAuthentication,
                            projectId: ProjectId)
  case class StopScheduler(uuid: UUID)
  case object StopAllSchedulers
  case class SchedulerStarted(uuid: UUID)
}

class JiraTagParseScheduler(wsClient: WSClient, systemServices: SystemServices)
    extends Actor
    with ActorLogging {
  import JiraTagParseScheduler._
  var workers: Map[UUID, ActorRef] = Map()

  override val supervisorStrategy: OneForOneStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
      case _ => Restart
    }

  val receive: Receive = {
    case StartScheduler(config, settings, projectSettings, auth, projectId) =>
      log.debug(
        s"StartScheduler: $config, $auth, $projectId, ${projectSettings.jiraProjectKey}")
      val uuid = UUID.randomUUID
      val ref = context.actorOf(
        JiraTagParseWorker.props(wsClient,
                                 systemServices,
                                 config,
                                 settings,
                                 projectSettings,
                                 auth,
                                 projectId))
      workers += uuid -> ref
      ref ! StartParsing
      sender() ! SchedulerStarted(uuid)
    case StopScheduler(uuid) =>
      log.debug(s"StopScheduler: $uuid")
      workers = workers
        .get(uuid)
        .map { worker =>
          log.debug(s"Stopping worker with uuid:$uuid")
          worker ! PoisonPill
          workers - uuid
        }
        .getOrElse(workers)

    case StopAllSchedulers =>
      log.debug("Stopping all workers")
      workers.map { case (_, worker) => worker ! PoisonPill }
  }
}
