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

trait MongoSetup extends EmbedConnection {
  self: Specification =>
  val rnd = new scala.util.Random
  val range = 12000 to 12999
  val portNum = range(rnd.nextInt(range length))

  override def embedConnectionPort() = { portNum }

  def withMongo[T](code: => T) = {
    running(FakeApplication(additionalConfiguration =
      Map(
        ("mongodb.uri", "mongodb://localhost:" + embedConnectionPort().toString + "/" + BSONObjectID.generate.stringify),
        ("mongodb.channels", "1"))))(code)
  }
}

class WithMongo extends Around with Scope {

  lazy val rnd = new scala.util.Random
  lazy val range = 12000 to 12999
  lazy val port = range(rnd.nextInt(range length))

  implicit val executionContext = ExecutionContext.Implicits.global

  private val driver = new MongoDriver

  val nodes = List(s"localhost:$port")

  private lazy val connection = driver.connection(nodes)

  lazy val db = connection("test")

  lazy val mongodConfig = new MongodConfigBuilder()
    .version(Version.Main.V2_6)
    .net(new Net(port, Network.localhostIsIPv6))
    .build

  lazy val logger = Logger.getLogger(getClass().getName());

  lazy val processOutput = new ProcessOutput(Processors.logTo(logger, Level.FINEST), Processors.logTo(logger,
    Level.FINEST), Processors.named("[console>]", Processors.logTo(logger, Level.FINEST)));

  lazy val runtimeConfig: IRuntimeConfig = new RuntimeConfigBuilder()
    .defaultsWithLogger(Command.MongoD, logger)
    .processOutput(processOutput)
    .artifactStore(new ArtifactStoreBuilder()
      .defaults(Command.MongoD)
      .download(new DownloadConfigBuilder()
        .defaultsForCommand(Command.MongoD)
        .progressListener(new LoggingProgressListener(logger, Level.FINEST))))
    .build;

  lazy val runtime = MongodStarter.getInstance(runtimeConfig)
  lazy val mongodExecutable = runtime.prepare(mongodConfig)

  def start = mongodExecutable.start

  def stop = mongodExecutable.stop

  override def around[T: AsResult](t: => T): Result = {
    logger.info(s"Start MongoDB on port:$port")
    start
    try {
      logger.info(s"Execute test with mongodb on port:$port")
      running(FakeApplication(additionalConfiguration =
        Map(
          ("mongodb.uri", "mongodb://localhost:" + port.toString + "/" + BSONObjectID.generate.stringify),
          ("mongodb.channels", "1"))))(AsResult(t))
    } finally {
      logger.info(s"Stop MongoDB on port:$port")
      stop
    }
  }
}