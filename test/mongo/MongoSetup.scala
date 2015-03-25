package mongo

import play.api.test.Helpers._
import play.api.test.FakeApplication
import reactivemongo.bson.BSONObjectID
import com.github.athieriot.EmbedConnection
import org.specs2.mutable.Specification

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
