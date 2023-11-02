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

import play.api.cache.SyncCacheApi

import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

class MockSyncCache extends SyncCacheApi {
  var cacheMap: Map[String, Any] = Map()
  override def set(key: String, value: Any, expiration: Duration): Unit =
    cacheMap = cacheMap + (key -> value)

  override def remove(key: String): Unit = cacheMap = cacheMap - key

  override def getOrElseUpdate[A: ClassTag](key: String, expiration: Duration)(
      orElse: => A): A = cacheMap.getOrElse(key, orElse).asInstanceOf[A]

  override def get[T: ClassTag](key: String): Option[T] =
    cacheMap.get(key).asInstanceOf[Option[T]]

  def removeAll(): Unit = {
    cacheMap = Map()
  }
}
