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
import org.specs2.matcher.Scope
import org.specs2.mutable.Around
import org.specs2.execute.AsResult
import org.specs2.execute.Result
import de.flapdoodle.embed.process.io.Processors
import de.flapdoodle.embed.mongo.config.ArtifactStoreBuilder
import de.flapdoodle.embed.mongo.config.DownloadConfigBuilder
import de.flapdoodle.embed.process.io.progress.LoggingProgressListener
import org.specs2.mutable.BeforeAfter
import play.modules.reactivemongo.ReactiveMongoApi
import play.api.test.PlayRunners
import play.api.Play
import de.flapdoodle.embed.mongo.config.processlistener.IMongoProcessListener
import mongo.EmbedMongo.MongoConfig
import org.specs2.specification.core.Fragments
import org.specs2.specification.Step
import de.flapdoodle.embed.mongo.config.ExtractedArtifactStoreBuilder
import org.specs2.specification.BeforeAfterAll
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import de.flapdoodle.embed.process.io.Slf4jLevel

trait EmbedMongo extends Specification with EmbedConnection {
  private lazy val rnd = new scala.util.Random
  private lazy val range = 12000 to 12999
  private lazy val port = range(rnd.nextInt(range length))
  
  implicit val executionContext = ExecutionContext.Implicits.global
  
  override def embedConnectionPort = port
  implicit val config = MongoConfig(port)
}

object EmbedMongo {
  case class MongoConfig(port: Int)

  class WithMongo(implicit val config: MongoConfig) extends Around with Scope {

    println(s"WithMongo:$config")
    
    lazy val dbName = BSONObjectID.generate.stringify

    lazy val logger = LoggerFactory.getLogger(getClass().getName());

    override def around[T: AsResult](t: => T): Result = {
      println(s"around:$t")
      val port = config.port;
      logger.warn(s"Execute test with mongodb on port:${port}")
      implicit lazy val app = FakeApplication(additionalConfiguration =
        Map(
          ("mongodb.uri", s"mongodb://localhost:${port}/${dbName}"),
          ("mongodb.channels", "1"),
          ("akka.contrib.persistence.mongodb.mongo.urls", List(s"localhost:${port}")),
          ("akka.contrib.persistence.mongodb.mongo.db", dbName)))

      logger.warn("Run with application:" + app)
      AsResult(running(app)(t))
    }
  }
}

