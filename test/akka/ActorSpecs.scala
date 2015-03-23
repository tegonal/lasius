package akka

import domain._
import org.specs2.mutable.{ SpecificationLike, Specification }
import collection.JavaConversions._
import org.specs2.specification.{ SpecificationStructure, Step, Fragments }
import org.specs2.control.StackTraceFilter
import akka.testkit.TestKit
import akka.actor.ActorSystem
import akka.util.Timeout
import akka.testkit.DefaultTimeout
import akka.testkit.ImplicitSender
import org.specs2.time.NoTimeConversions

abstract class ActorSpecs extends TestKit(ActorSystem()) with SpecificationLike with DefaultTimeout with ImplicitSender with NoTimeConversions {

  isolated

  // https://groups.google.com/d/topic/specs2-users/PdCeX4zxc0A/discussion
  override def map(fs: => Fragments) = super.map(fs) ^ Step(system.shutdown())
}