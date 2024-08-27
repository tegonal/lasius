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

import actors.scheduler.gitlab.GitlabTagParseScheduler
import actors.scheduler.jira.JiraTagParseScheduler
import actors.scheduler.plane.PlaneTagParseScheduler
import actors.scheduler.{
  ApiKeyAuthentication,
  OAuth2Authentication,
  ServiceConfiguration
}
import akka.actor._
import core.LoginHandler.InitializeUserViews
import play.api.libs.ws.WSClient
import play.modules.reactivemongo.ReactiveMongoApi
import repositories._

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

object PluginHandler {
  def props(userRepository: UserRepository,
            jiraConfigRepository: JiraConfigRepository,
            gitlabConfigRepository: GitlabConfigRepository,
            planeConfigRepository: PlaneConfigRepository,
            systemServices: SystemServices,
            wsClient: WSClient,
            reactiveMongoApi: ReactiveMongoApi): Props =
    Props(classOf[PluginHandler],
          userRepository,
          jiraConfigRepository,
          gitlabConfigRepository,
          planeConfigRepository,
          systemServices,
          wsClient,
          reactiveMongoApi)

  case object Startup

  case object Shutdown
}

class PluginHandler(userRepository: UserRepository,
                    jiraConfigRepository: JiraConfigRepository,
                    gitlabConfigRepository: GitlabConfigRepository,
                    planeConfigRepository: PlaneConfigRepository,
                    systemServices: SystemServices,
                    wsClient: WSClient,
                    override val reactiveMongoApi: ReactiveMongoApi)
    extends Actor
    with ActorLogging
    with ConfigAware
    with DBSupport {

  override val supportTransaction: Boolean = systemServices.supportTransaction

  import PluginHandler._

  implicit val executionContext: ExecutionContextExecutor = context.dispatcher

  val jiraTagParseScheduler: ActorRef =
    context.actorOf(JiraTagParseScheduler.props(wsClient, systemServices))
  val gitlabTagParseScheduler: ActorRef =
    context.actorOf(GitlabTagParseScheduler.props(wsClient, systemServices))
  val planeTagParseScheduler: ActorRef =
    context.actorOf(PlaneTagParseScheduler.props(wsClient, systemServices))

  log.debug(s"PluginHandler started")

  val receive: Receive = {
    case Startup =>
      log.debug(s"PluginHandler startup")
      withDBSession() { implicit dbSession =>
        Future {
          initialize()
        }
      }
    case Shutdown =>
    case e =>
      log.warning(s"Received unknown event:$e")
  }

  def initialize()(implicit dbSession: DBSession): Unit = {
    initializeUserViews()
    initializeGitlabPlugin()
    initializeJiraPlugin()
    initializePlanePlugin()
  }

  private def initializeUserViews()(implicit dbSession: DBSession): Unit = {
    val initializeViews = config
      .getBoolean("lasius.persistence.on_startup.initialize_views")
    log.debug(s"initializeUserViews:$initializeViews")
    if (initializeViews) {
      userRepository.findAll().foreach { users =>
        log.debug(s"findAllUsers:${users.map(_.getReference())}")
        users.foreach(user =>
          systemServices.loginHandler ! InitializeUserViews(
            user.getReference()))
      }
    }
  }

  private def initializeJiraPlugin()(implicit dbSession: DBSession): Unit = {
    log.debug(s"PluginHandler initializeJiraPlugin:$jiraConfigRepository")
    // start jira parse scheduler for every project attached to a jira configuration
    jiraConfigRepository.getJiraConfigurations
      .map { s =>
        log.debug(s"Got jira configs:$s")
        s.map { config =>
          log.debug(s"Start Jira Scheduler for config:$config")
          val jiraConfig = ServiceConfiguration(config.baseUrl.toString)
          val auth = OAuth2Authentication(config.auth.consumerKey,
                                          config.auth.privateKey,
                                          config.auth.accessToken)

          config.projects.map { proj =>
            log.debug(
              s"Start parsing for the following configuration:$jiraConfig - $proj")
            jiraTagParseScheduler ! JiraTagParseScheduler.StartScheduler(
              jiraConfig,
              config.settings,
              proj.settings,
              auth,
              proj.projectId)
          }
        }
      }
      .onComplete {
        case Success(_) =>
          log.debug(s"Successfully loaded jira plugins")
        case Failure(exception) =>
          log.warning(s"Failed loading jira configuration", exception)
      }
    ()
  }

  private def initializeGitlabPlugin()(implicit dbSession: DBSession): Unit = {
    log.debug(s"PluginHandler initializeGitlabPlugin:$gitlabConfigRepository")
    // start gitlab parse scheduler for every project attached to a gitlab configuration
    gitlabConfigRepository.getGitlabConfigurations
      .map { s =>
        log.debug(s"Got gitlab configs:$s")
        s.map { config =>
          log.debug(s"Start Gitlab Scheduler for config:$config")
          val serviceConfig = ServiceConfiguration(config.baseUrl.toString)
          val auth = OAuth2Authentication(config.auth.consumerKey,
                                          config.auth.privateKey,
                                          config.auth.accessToken)

          config.projects.map { proj =>
            log.debug(
              s"Start parsing for the following configuration:$serviceConfig - $proj")
            gitlabTagParseScheduler ! GitlabTagParseScheduler.StartScheduler(
              serviceConfig,
              config.settings,
              proj.settings,
              auth,
              proj.projectId)
          }
        }
      }
      .onComplete {
        case Success(_) =>
          log.debug(s"Successfully loaded gitlab plugins")
        case Failure(exception) =>
          log.warning(s"Failed loading gitlab configuration", exception)
      }
    ()
  }

  private def initializePlanePlugin()(implicit dbSession: DBSession): Unit = {
    log.debug(s"PluginHandler initializePlanePlugin:$planeConfigRepository")
    // start plane parse scheduler for every project attached to a plane configuration
    planeConfigRepository.getPlaneConfigurations
      .map { s =>
        log.debug(s"Got plane configs:$s")
        s.map { config =>
          log.debug(s"Start Plane Scheduler for config:$config")
          val serviceConfig = ServiceConfiguration(config.baseUrl.toString)
          val auth          = ApiKeyAuthentication(config.auth.apiKey)

          config.projects.map { proj =>
            log.debug(
              s"Start parsing for the following configuration:$serviceConfig - $proj")
            planeTagParseScheduler ! PlaneTagParseScheduler.StartScheduler(
              serviceConfig,
              config.settings,
              proj.settings,
              auth,
              proj.projectId)
          }
        }
      }
      .onComplete {
        case Success(_) =>
          log.debug(s"Successfully loaded plane plugins")
        case Failure(exception) =>
          log.warning(s"Failed loading plane configuration", exception)
      }
    ()
  }
}
