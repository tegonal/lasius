package controllers

import net.sf.ehcache.CacheManager
import play.api.cache.ehcache.SyncEhCacheApi
import play.api.cache.SyncCacheApi

trait CacheAware {
  val cache: SyncCacheApi
}

trait DefaultCacheProvider extends CacheAware {
  override lazy val cache: SyncCacheApi = {
    CacheManager.getInstance().addCache("default")
    new SyncEhCacheApi(CacheManager.getInstance().getCache("default"))
  }
}