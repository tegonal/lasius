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
import de.flapdoodle.embed.mongo.config.{Defaults, MongodConfig, Net}
import de.flapdoodle.embed.mongo.distribution.Version
import de.flapdoodle.embed.mongo.MongodStarter
import de.flapdoodle.embed.mongo.packageresolver.Command
import de.flapdoodle.embed.process.config.RuntimeConfig
import de.flapdoodle.embed.process.config.process.ProcessOutput
import de.flapdoodle.embed.process.io.{Processors, Slf4jLevel}
import de.flapdoodle.embed.process.runtime.Network
import org.slf4j.LoggerFactory
import org.specs2.execute.{AsResult, Result}
import org.specs2.mutable.{Specification, SpecificationLike}
import org.specs2.specification.AroundEach
import play.api.inject.guice.GuiceApplicationBuilder
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.bson.BSONObjectID
import util.Awaitable

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}
import scala.language.postfixOps

object LazyMongo {

  lazy val mongo = {
    lazy val rnd    = new scala.util.Random
    lazy val range  = 12000 to 12999
    lazy val port   = range(rnd.nextInt(range length))
    lazy val dbName = BSONObjectID.generate().stringify

    implicit val executionContext = ExecutionContext.Implicits.global

    lazy val mongodConfig = MongodConfig
      .builder()
      .version(Version.Main.PRODUCTION)
      .net(new Net(port, Network.localhostIsIPv6()))
      .build()

    lazy val logger = LoggerFactory.getLogger(getClass().getName())

    lazy val processOutput = ProcessOutput
      .builder()
      .commands(Processors.logTo(logger, Slf4jLevel.TRACE))
      .error(Processors.logTo(logger, Slf4jLevel.TRACE))
      .output(Processors.named("[console>]",
                               Processors.logTo(logger, Slf4jLevel.TRACE)))
      .build();

    val command = Command.MongoD

    lazy val runtimeConfig: RuntimeConfig = Defaults
      .runtimeConfigFor(command, logger)
      .processOutput(processOutput)
      .artifactStore(
        Defaults
          .extractedArtifactStoreFor(command)
          .withDownloadConfig(Defaults.downloadConfigFor(command).build))
      .build

    lazy val runtime          = MongodStarter.getInstance(runtimeConfig)
    lazy val mongodExecutable = runtime.prepare(mongodConfig)

    logger.info(s"Start mongo on port:$port")
    val proc = mongodExecutable.start
    logger.info(s"Started mongo on port:$port:${proc.isProcessRunning()}")

    implicit lazy val app = new GuiceApplicationBuilder()
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
  implicit val executionContext = ExecutionContext.Implicits.global
  override val reactiveMongoApi = LazyMongo.mongo

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
