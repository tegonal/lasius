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

package mongo

import core.TestDBSupport
import de.flapdoodle.embed.mongo.config.Net
import de.flapdoodle.embed.mongo.distribution.Version
import de.flapdoodle.embed.mongo.transitions.Mongod
import de.flapdoodle.embed.process.io.{ProcessOutput, Processors, Slf4jLevel}
import de.flapdoodle.reverse.Transition
import de.flapdoodle.reverse.transitions.Start
import org.slf4j.LoggerFactory
import org.specs2.execute.{AsResult, Result}
import org.specs2.mutable.SpecificationLike
import org.specs2.specification.AroundEach
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.bson.BSONObjectID
import util.Awaitable

import scala.concurrent.ExecutionContext
import scala.language.postfixOps

object LazyMongo {

  lazy val mongo: ReactiveMongoApi = {
    lazy val rnd    = new scala.util.Random
    lazy val range  = 12000 to 12999
    lazy val port   = range(rnd.nextInt(range length))
    lazy val dbName = BSONObjectID.generate().stringify

    lazy val logger = LoggerFactory.getLogger(getClass.getName)

    logger.info(s"Start mongo on port:$port")

    val mongod = new Mongod() {
      override def processOutput: Transition[ProcessOutput] = Start
        .to(classOf[ProcessOutput])
        .initializedWith(
          ProcessOutput.builder
            .output(
              Processors.named("[console>]",
                               Processors.logTo(logger, Slf4jLevel.TRACE)))
            .error(Processors.logTo(logger, Slf4jLevel.TRACE))
            .commands(Processors.logTo(logger, Slf4jLevel.TRACE))
            .build)
        .withTransitionLabel("create named console")

      override def net(): Transition[Net] =
        Start.to(classOf[Net]).initializedWith(Net.defaults.withPort(port))
    }
    mongod.start(Version.Main.V4_4)

    implicit lazy val app: Application = new GuiceApplicationBuilder()
      .configure(Map(
        ("mongodb.uri",
         s"mongodb://localhost:$port/$dbName?w=majority&readConcernLevel=majority&maxPoolSize=1&rm.nbChannelsPerNode=1"),
        ("mongodb.channels", "1"),
        ("akka.contrib.persistence.mongodb.mongo.urls",
         List(s"localhost:$port")),
        ("akka.contrib.persistence.mongodb.mongo.db", dbName)
      ))
      .build()

    lazy val reactiveMongoApi = app.injector.instanceOf[ReactiveMongoApi]

    reactiveMongoApi
  }

}

trait EmbedMongo
    extends SpecificationLike
    with AroundEach
    with Awaitable
    with TestDBSupport {
  sequential =>
  implicit val executionContext: ExecutionContext =
    ExecutionContext.Implicits.global
  override val reactiveMongoApi: ReactiveMongoApi = LazyMongo.mongo

  override protected def around[R](r: => R)(implicit
      evidence$1: AsResult[R]): Result = {
    reactiveMongoApi.database
      .flatMap(_.drop())
      .map { _ =>
        AsResult(r)
      }
      .awaitResult()
  }
}
