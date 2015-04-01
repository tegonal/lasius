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
import org.specs2.specification.Fragments
import org.specs2.specification.Step
import org.specs2.mutable.script.SpecificationLike
import mongo.EmbedMongo.MongoConfig

trait EmbedMongo extends Specification {

  private lazy val rnd = new scala.util.Random
  private lazy val range = 12000 to 12999
  private lazy val port = range(rnd.nextInt(range length))

  implicit val executionContext = ExecutionContext.Implicits.global

  lazy val nodes = List(s"localhost:$port")

  lazy val mongodConfig = new MongodConfigBuilder()
    .version(Version.V2_6_1)
    .net(new Net(port, Network.localhostIsIPv6()))
    .build

  lazy val logger = Logger.getLogger(getClass().getName());

  lazy val processOutput = new ProcessOutput(Processors.logTo(logger, Level.FINEST), Processors.logTo(logger,
    Level.FINEST), Processors.named("[console>]", Processors.logTo(logger, Level.FINEST)));

  lazy val runtimeConfig: IRuntimeConfig = new RuntimeConfigBuilder()
    .defaultsWithLogger(Command.MongoD, logger)
    .processOutput(processOutput)
    .artifactStore(new ArtifactStoreBuilder()
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
    Step(start) ^ fragments ^ Step(stop)
  }
}

object EmbedMongo {
  case class MongoConfig(port: Int)

  class WithMongo(implicit val config: MongoConfig) extends Around with Scope {

    lazy val dbName = BSONObjectID.generate.stringify

    lazy val logger = Logger.getLogger(getClass().getName());

    override def around[T: AsResult](t: => T): Result = {
      val port = config.port;
      logger.info(s"Execute test with mongodb on port:${port}")
      implicit lazy val app = FakeApplication(additionalConfiguration =
        Map(
          ("mongodb.uri", s"mongodb://localhost:${port}/${dbName}"),
          ("mongodb.channels", "1"),
          ("akka.contrib.persistence.mongodb.mongo.urls", List(s"localhost:${port}")),
          ("akka.contrib.persistence.mongodb.mongo.db", dbName)))

      logger.info("Run with application:" + app)
      AsResult(running(app)(t))
    }
  }
}

