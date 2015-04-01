package akka

import akka.testkit.TestKit
import akka.actor.ActorSystem
import org.specs2.matcher.Scope
import org.specs2.mutable.Around
import org.specs2.execute.AsResult
import org.specs2.execute.Result
import org.specs2.mutable.Specification
import mongo.EmbedMongo
import mongo.EmbedMongo.MongoConfig
import mongo.EmbedMongo.WithMongo

class ActorTestScope extends TestKit(ActorSystem("test")) with Scope

abstract class PersistentActorTestScope(implicit val config: MongoConfig) extends ActorTestScope with Around {
  lazy val withMongo = new WithMongo

  override def around[T: AsResult](t: => T): Result = {
    withMongo.around(t)
  }
}

