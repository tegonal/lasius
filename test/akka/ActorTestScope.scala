package akka

import mongo.MongoSetup
import akka.testkit.TestKit
import akka.actor.ActorSystem
import org.specs2.matcher.Scope
import org.specs2.mutable.Around
import org.specs2.execute.AsResult
import org.specs2.execute.Result
import org.specs2.mutable.Specification
import mongo.MongoSetup
import mongo.MongoSetup

class ActorTestScope extends TestKit(ActorSystem("test")) with Scope


abstract class PersistentActorTestScope(implicit val mongoSetup:MongoSetup) extends ActorTestScope with Around {
  override def around[T: AsResult](t: => T): Result = {
    mongoSetup.withMongo(AsResult(t))
  }
}

class PersistentActorSpecification extends Specification with MongoSetup {
  isolated
  implicit val mongoSetup:MongoSetup = this
}