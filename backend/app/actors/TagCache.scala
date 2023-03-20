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

package actors

import akka.actor._
import core.SystemServices
import models._
import shapeless._

object TagCache {

  case class GetTags(projectId: ProjectId)

  case class CachedTags(projectId: ProjectId, tags: Set[Tag])

  case class TagsUpdated[X <: Tag](externalProjectId: String,
                                   projectId: ProjectId,
                                   tags: Set[X])(implicit m: Manifest[X]) {
    val manifest: Manifest[X] = m
  }

  def props(systemServices: SystemServices,
            clientReceiver: ClientReceiver): Props =
    Props(classOf[TagCache], systemServices, clientReceiver)
}

class TagCache(systemServices: SystemServices, clientReceiver: ClientReceiver)
    extends Actor
    with ActorLogging {

  import TagCache._

  import scala.reflect.runtime.universe._

  var tagCache: Map[ProjectId, Map[String, Map[Manifest[_], Set[Tag]]]] =
    Map()

  val receive: Receive = {
    case t @ TagsUpdated(externalProjectId, projectId, tags) =>
      adjustCache(t.manifest, externalProjectId, projectId, tags)
    case GetTags(projectId) =>
      val projectTags = tagCache
        .get(projectId)
        .map(_.flatMap(_._2.flatMap(_._2)).toSet)
      sender() ! CachedTags(projectId, projectTags.getOrElse(Set()))
  }

  def adjustCache[T <: Tag: TypeTag](typ: Manifest[T],
                                     externalProjectId: String,
                                     projectId: ProjectId,
                                     tags: Set[Tag]): Unit = {
    // Needed to allow V to be inferred in Case1 resolution (ie. map sand pairApply)
    implicit object MS extends (Manifest ~?> Set)

    val currentMap = tagCache.get(projectId).flatMap(_.get(externalProjectId))
    val current    = currentMap.map(_.getOrElse(typ, Set())).getOrElse(Set())

    val diff = calcDiff(current, tags)

    // notify client
    log.debug(s"TagCache changed:$diff")
    tagCache = diff
      .map { case (removed, added) =>
        val msg = TagCacheChanged(projectId, removed, added)
        clientReceiver.broadcast(systemServices.systemUser, msg)

        // update cache
        val newMap = currentMap.map(_ + (typ -> tags)).getOrElse(Map())

        val allProjectTags = tagCache
          .get(projectId)
          .getOrElse(Map()) + (externalProjectId -> newMap)
        tagCache + (projectId                    -> allProjectTags)
      }
      .getOrElse(tagCache)
  }

  def calcDiff(current: Set[Tag],
               tags: Set[Tag]): Option[(Set[Tag], Set[Tag])] = {
    // find tags removed
    val removed = diff(current, tags)

    // tag tags added
    val added = diff(tags, current)

    if ((removed.size + added.size) > 0)
      Some((removed, added))
    else
      None
  }

  def diff(current: Set[Tag], updated: Set[Tag]): Set[Tag] =
    current -- (updated)
}
