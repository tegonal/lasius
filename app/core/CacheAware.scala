package core

import play.api.cache.SyncCacheApi

trait CacheAware {
  val cache: SyncCacheApi
}

trait DefaultCacheAware extends CacheAware {
  val cache: SyncCacheApi = DefaultCacheProvider.getInstance().cache
}