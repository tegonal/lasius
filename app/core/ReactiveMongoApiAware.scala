package core

import play.modules.reactivemongo.ReactiveMongoApi

trait ReactiveMongoApiAware {
  val reactiveMongoApi: ReactiveMongoApi
}

trait DefaultReactiveMongoApiAware extends ReactiveMongoApiAware {
  override lazy val reactiveMongoApi: ReactiveMongoApi = DefaultReactiveMongoApi.getInstance().reactiveMongoApi
}