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

import akka.Done
import play.api.cache.{AsyncCacheApi, SyncCacheApi}

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

trait MockCacheAware extends CacheAware {
  override val cache: AsyncCacheApi = MockCache
}

object MockCache extends AsyncCacheApi {
  var cacheMap: Map[String, Any] = Map()

  override def set(key: String,
                   value: Any,
                   expiration: Duration): Future[Done] = {
    cacheMap = cacheMap + (key -> value)
    Future.successful(Done)
  }

  override def remove(key: String): Future[Done] = {
    cacheMap = cacheMap - key
    Future.successful(Done)
  }

  override def getOrElseUpdate[A](key: String, expiration: Duration)(
      orElse: => Future[A])(implicit evidence$1: ClassTag[A]): Future[A] = {
    Future.successful(cacheMap.get(key).getOrElse(orElse).asInstanceOf[A])
  }

  override def get[A](key: String)(implicit
      evidence$2: ClassTag[A]): Future[Option[A]] = {
    Future.successful(cacheMap.get(key).asInstanceOf[Option[A]])
  }

  override def removeAll(): Future[Done] = {
    cacheMap = Map()
    Future.successful(Done)
  }
}
