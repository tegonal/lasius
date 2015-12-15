/*   __                          __                                          *\
*   / /____ ___ ____  ___  ___ _/ /       lasius                      *
*  / __/ -_) _ `/ _ \/ _ \/ _ `/ /        contributed by tegonal              *
*  \__/\__/\_, /\___/_//_/\_,_/_/         http://tegonal.com/                 *
*         /___/                                                               *
*                                                                             *
* This program is free software: you can redistribute it and/or modify it     *
* under the terms of the GNU General Public License as published by    *
* the Free Software Foundation, either version 3 of the License,              *
* or (at your option) any later version.                                      *
*                                                                             *
* This program is distributed in the hope that it will be useful, but         *
* WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY  *
* or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for *
* more details.                                                               *
*                                                                             *
* You should have received a copy of the GNU General Public License along     *
* with this program. If not, see http://www.gnu.org/licenses/                 *
*                                                                             *
\*                                                                           */
package mongo

import play.api.test.Helpers._
import play.api.test.FakeApplication
import reactivemongo.bson.BSONObjectID
import com.github.athieriot.EmbedConnection
import org.specs2.mutable.Specification
import scala.concurrent.ExecutionContext
import de.flapdoodle.embed.mongo.{ Command, MongodStarter }
import de.flapdoodle.embed.mongo.config.{ MongodConfigBuilder, Net, RuntimeConfigBuilder }
import de.flapdoodle.embed.mongo.distribution.Version
import de.flapdoodle.embed.process.config.IRuntimeConfig
import de.flapdoodle.embed.process.config.io.ProcessOutput
import de.flapdoodle.embed.process.runtime.Network
import reactivemongo.api.MongoDriver
import java.util.logging.Logger
import org.specs2.matcher.Scope
import org.specs2.mutable.Around
import org.specs2.execute.AsResult
import org.specs2.execute.Result
import de.flapdoodle.embed.process.io.Processors
import java.util.logging.Level
import de.flapdoodle.embed.mongo.config.ArtifactStoreBuilder
import de.flapdoodle.embed.mongo.config.DownloadConfigBuilder
import de.flapdoodle.embed.process.io.progress.LoggingProgressListener
import org.specs2.mutable.BeforeAfter
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.api.test.PlayRunners
import play.api.Play
import de.flapdoodle.embed.mongo.config.processlistener.IMongoProcessListener
import mongo.EmbedMongo.MongoConfig
import org.specs2.specification.core.Fragments
import org.specs2.specification.Step
import de.flapdoodle.embed.mongo.config.ExtractedArtifactStoreBuilder

trait EmbedMongo extends Specification {
  sequential =>

  private lazy val rnd = new scala.util.Random
  private lazy val range = 12000 to 12999
  private lazy val port = range(rnd.nextInt(range length))

  implicit val executionContext = ExecutionContext.Implicits.global

  lazy val nodes = List(s"localhost:$port")

  lazy val mongodConfig = new MongodConfigBuilder()
    .version(Version.Main.PRODUCTION)
    .net(new Net(port, Network.localhostIsIPv6()))
    .build

  lazy val logger = Logger.getLogger(getClass().getName());

  lazy val processOutput = new ProcessOutput(Processors.logTo(logger, Level.FINEST), Processors.logTo(logger,
    Level.FINEST), Processors.named("[console>]", Processors.logTo(logger, Level.FINEST)));

  lazy val runtimeConfig: IRuntimeConfig = new RuntimeConfigBuilder()
    .defaultsWithLogger(Command.MongoD, logger)
    .processOutput(processOutput)
    .artifactStore(new ExtractedArtifactStoreBuilder()
      .defaults(Command.MongoD))
    .build;

  lazy val runtime = MongodStarter.getInstance(runtimeConfig)
  lazy val mongodExecutable = runtime.prepare(mongodConfig)

  def start = {
    logger.info(s"Start mongo on port:${port}")
    val proc = mongodExecutable.start
    logger.info(s"Started mongo on port:${port}:${proc.isProcessRunning()}")
  }

  def stop = {
    logger.info(s"Stop mongo on port:${port}")
    mongodExecutable.stop
    logger.info(s"Stopped mongo on port:${port}")
  }

  implicit val config = MongoConfig(port)

  override def map(fragments: => Fragments) = {
    step(start) ^ fragments ^ step(stop)
  }
}

object EmbedMongo {
  case class MongoConfig(port: Int)

  class WithMongo(implicit val config: MongoConfig) extends Around with Scope {

    lazy val dbName = BSONObjectID.generate.stringify

    lazy val logger = Logger.getLogger(getClass().getName());

    override def around[T: AsResult](t: => T): Result = {
      val port = config.port;
      logger.warning(s"Execute test with mongodb on port:${port}")
      implicit lazy val app = FakeApplication(additionalConfiguration =
        Map(
          ("mongodb.uri", s"mongodb://localhost:${port}/${dbName}"),
          ("mongodb.channels", "1"),
          ("akka.contrib.persistence.mongodb.mongo.urls", List(s"localhost:${port}")),
          ("akka.contrib.persistence.mongodb.mongo.db", dbName)))

      logger.warning("Run with application:" + app)
      AsResult(running(app)(t))
    }
  }
}

