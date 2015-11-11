package domain.views

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.contrib.persistence.mongodb.MongoReadJournal
import akka.contrib.persistence.mongodb.ScalaDslMongoReadJournal
import akka.persistence.query.PersistenceQuery
import akka.stream.scaladsl.Source
import akka.persistence.query.EventEnvelope
import akka.stream.ActorMaterializer

abstract class PersistenceViewWrapper extends Actor with ActorLogging {
  val persistenceId: String

  val readJournal = PersistenceQuery(context.system).readJournalFor[ScalaDslMongoReadJournal](MongoReadJournal.Identifier)
  val source: Source[EventEnvelope, Unit] = readJournal.currentEventsByPersistenceId(persistenceId, 0, Long.MaxValue)
  implicit val mat = ActorMaterializer()
  source.runForeach { event =>
    log.error(s"EVENT: sevent")
    if (event != null && event.event != null) {
      receive(event.event)
    }
  }
}