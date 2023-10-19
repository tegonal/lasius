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

package models.adapters

import akka.actor.ExtendedActorSystem
import akka.persistence.journal.{EventSeq, ReadEventAdapter}
import core.{DBSession, DBSupport, PlayAkkaExtension}
import models._
import play.modules.reactivemongo.ReactiveMongoApi
import repositories.{ProjectRepository, UserRepository}

import scala.annotation.nowarn
import scala.concurrent.{Await, Awaitable, ExecutionContextExecutor}
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

class PersistedEventAdapter(system: ExtendedActorSystem)
    extends ReadEventAdapter {

  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  lazy val allUsers = {
    val reactiveMongoApi = PlayAkkaExtension(system)
      .instanceOf[ReactiveMongoApi](classOf[ReactiveMongoApi])
    val userRepository: UserRepository =
      PlayAkkaExtension(system).instanceOf[UserRepository](
        classOf[UserRepository])

    Await.result(DBSession
                   .start(reactiveMongoApi, withTransaction = false)
                   .flatMap { implicit dbSession =>
                     userRepository.findAll()
                   },
                 30 seconds)
  }

  lazy val allProjects = {
    val reactiveMongoApi = PlayAkkaExtension(system)
      .instanceOf[ReactiveMongoApi](classOf[ReactiveMongoApi])
    val projectRepository: ProjectRepository =
      PlayAkkaExtension(system).instanceOf[ProjectRepository](
        classOf[ProjectRepository])

    Await.result(DBSession
                   .start(reactiveMongoApi, withTransaction = false)
                   .flatMap { implicit dbSession =>
                     projectRepository.findAll().map(_.map(_._1).toSeq)
                   },
                 30 seconds)
  }

  override def fromJournal(event: Any, manifest: String): EventSeq =
    event match {
      case e: PersistedEvent =>
        EventSeq.single(updatePersistedEvent(e))
      case _ =>
        EventSeq.single(event)
    }

  @nowarn("cat=deprecation")
  def updatePersistedEvent(event: PersistedEvent): PersistedEvent = {
    event match {
      case e: UserTimeBookingStarted     => e.toV2(allUsers, allProjects)
      case e: UserTimeBookingStopped     => e.toV2(allUsers, allProjects)
      case e: UserTimeBookingRemoved     => e.toV2(allUsers, allProjects)
      case e: UserTimeBookingAdded       => e.toV2(allUsers, allProjects)
      case e: UserTimeBookingEdited      => e.toV2().toV3(allUsers, allProjects)
      case e: UserTimeBookingEditedV2    => e.toV3(allUsers, allProjects)
      case e: UserLoggedIn               => e.toV2(allUsers)
      case e: UserLoggedOut              => e.toV2(allUsers)
      case e: UserTimeBookingInitialized => e.toV2(allUsers)
      case e                             => e
    }
  }
}
