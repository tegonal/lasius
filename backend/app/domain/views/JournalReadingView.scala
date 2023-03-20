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

package domain.views

import akka.NotUsed
import akka.actor.SupervisorStrategy.Restart
import akka.actor.{Actor, ActorLogging, OneForOneStrategy}
import akka.contrib.persistence.mongodb.{
  MongoReadJournal,
  ScalaDslMongoReadJournal
}
import akka.persistence.query.{EventEnvelope, PersistenceQuery}
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import domain.AggregateRoot.{InitializeViewLive, RestoreViewFromState}
import domain.UserTimeBookingAggregate.UserTimeBooking
import models.PersistedEvent

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

case object RestoreViewFromStateSuccess

case object JournalReadingViewIsLive

trait JournalReadingView extends Actor with ActorLogging {
  val persistenceId: String

  lazy val readJournal: ScalaDslMongoReadJournal =
    PersistenceQuery(context.system)
      .readJournalFor[ScalaDslMongoReadJournal](MongoReadJournal.Identifier)

  override val supervisorStrategy: OneForOneStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
      case _ =>
        Restart
    }

  def journalSource(fromSequenceNr: Long): Source[Any, NotUsed] =
    readJournal
      .currentEventsByPersistenceId(persistenceId,
                                    fromSequenceNr = fromSequenceNr,
                                    toSequenceNr = Long.MaxValue)
      .map((e: EventEnvelope) => {
        e.event match {
          case e => e
        }
      })

  def replayJournalSource(fromSequenceNr: Long): Unit = {
    implicit val materializer: Materializer =
      Materializer.matFromSystem(context.system)
    journalSource(fromSequenceNr).runForeach(event => context.self ! event)
  }

  val defaultReceive: Receive = {
    case RestoreViewFromState(userReference, sequenceNr, snapshot) =>
      log.debug(
        s"RestoreViewFromState: ${userReference.id}, $sequenceNr, $snapshot")
      restoreViewFromState(snapshot)
      sender() ! RestoreViewFromStateSuccess
      context.self ! InitializeViewLive(userReference, sequenceNr)

    case InitializeViewLive(userId, fromSequenceNr) =>
      log.debug(s"InitializeViewLive: $userId, $fromSequenceNr")
      context.become(live)
      replayJournalSource(fromSequenceNr)
      sender() ! JournalReadingViewIsLive
    case e: PersistedEvent =>
      log.debug(s"Received persistet event, need to switch to live mode")
      context.become(live)
      context.self ! e
    case e =>
      log.error(s"Unknown event: $e")
  }

  val receive: Receive = defaultReceive

  protected val live: Receive

  protected def restoreViewFromState(snapshot: UserTimeBooking): Unit
}
