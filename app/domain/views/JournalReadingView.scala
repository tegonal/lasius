package domain.views

import akka.actor.Actor
import akka.contrib.persistence.mongodb.{MongoReadJournal, ScalaDslMongoReadJournal}
import akka.persistence.query.PersistenceQuery
import akka.stream.ActorMaterializer
import akka.util.Timeout

import scala.concurrent.duration._

case object LiveStreamFailed

trait JournalReadingView extends Actor {
  val persistenceId: String

  lazy val readJournal =
    PersistenceQuery(context.system).readJournalFor[ScalaDslMongoReadJournal](MongoReadJournal.Identifier)

  lazy val journalSource = readJournal.eventsByPersistenceId(persistenceId, fromSequenceNr = 0L, toSequenceNr = Long.MaxValue)
    .map(_.event)

  override def preStart = {
    implicit val materializer = ActorMaterializer()
    implicit val timeout = Timeout(5 seconds)
    journalSource.runForeach(event => context.self ! event)
  }
}
