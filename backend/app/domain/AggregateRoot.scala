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

package domain

import akka.actor.SupervisorStrategy.Restart
import akka.actor._
import akka.pattern.StatusReply
import akka.persistence._
import domain.UserTimeBookingAggregate.UserTimeBooking
import domain.views.RestoreViewFromStateSuccess
import models.PersistedEvent
import models.UserId.UserReference

import scala.concurrent.duration._
import scala.language.postfixOps

object AggregateRoot {
  trait State

  trait Command

  case object GetState extends Command

  case class Initialize(state: State) extends Command

  case class ForwardPersistentEvent(userReference: UserReference,
                                    event: PersistedEvent)
      extends Command

  case class RestoreViewFromState(userReference: UserReference,
                                  sequenceNr: Long,
                                  state: UserTimeBooking)
      extends Command

  case class InitializeViewLive(userReference: UserReference,
                                fromSequenceNr: Long)
      extends Command

  case object Removed extends State

  case object Created extends State

  case object Uninitialized extends State
}

trait AggregateRoot extends PersistentActor with ActorLogging {

  import AggregateRoot._

  var state: State
  protected var snapshotReceived = false

  def updateState(evt: PersistedEvent): Unit

  def restoreFromSnapshot(metadata: SnapshotMetadata, state: State): Unit

  // snapshot after every x event
  protected val SnapshotInterval = 100
  // max number of snapshots to keep per aggregate
  protected val KeepSnapshots = 1

  protected var recovering = true

  override val supervisorStrategy: OneForOneStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
      case _ =>
        Restart
    }

  protected def afterEventPersisted(evt: PersistedEvent): Unit = {
    log.debug(
      s"afterEventPersisted lastSequenceNr=$lastSequenceNr, SnapshotInterval=$SnapshotInterval")
    updateState(evt)
    publish(evt)
    if (lastSequenceNr % SnapshotInterval == 0 && lastSequenceNr != 0) {
      if (KeepSnapshots >= 0) {
        val maxSeqNr = lastSequenceNr - (SnapshotInterval * KeepSnapshots)
        log.debug(s"delete old snapshots up to seqNr: $maxSeqNr")
        deleteSnapshots(SnapshotSelectionCriteria(maxSequenceNr = maxSeqNr))
      }
      log.debug(s"save snapshot")
      saveSnapshot(state)
    }
    log.debug(
      s"afterEventPersisted:send back state:$state in ${context.system}")
    sender() ! state
  }

  protected def publish(event: PersistedEvent): Unit =
    context.system.eventStream.publish(event)

  protected def afterRecoveryCompleted(sequenceNr: Long,
                                       state: State): Unit = {}

  protected val defaultReceive: Receive = {
    case StatusReply.Ack =>
    case _: SaveSnapshotSuccess =>
      log.debug("Successfully saved snapshot")
    case SaveSnapshotFailure(_, cause) =>
      log.warning("Saving snapshot failed", cause)
    case _: DeleteSnapshotSuccess =>
      log.debug("Successfully deleted snapshot")
    case DeleteSnapshotFailure(_, cause) =>
      log.warning("Deleting snapshot failed", cause)
    case _: DeleteSnapshotsSuccess =>
      log.debug("Successfully deleted snapshots")
    case DeleteSnapshotsFailure(_, cause) =>
      log.warning("Deleting snapshots failed", cause)
    case RestoreViewFromStateSuccess =>
      log.debug("Successfully restored view from state")
  }

  override val receiveRecover: Receive = ({
    case evt: PersistedEvent =>
      log.debug(s"receiveRecover:$evt")
      updateState(evt)
    case SnapshotOffer(metadata, state: State) =>
      snapshotReceived = true
      restoreFromSnapshot(metadata, state)
      log.debug("recovering aggregate from snapshot")
    case RecoveryCompleted =>
      maybePerformInitialSnapshot()
      log.debug("Recovery Completed")
  }: Receive).orElse(defaultReceive)

  private def maybePerformInitialSnapshot(): Unit = {
    if (!snapshotReceived && lastSequenceNr != 0) {
      saveSnapshot(state)
    }
    recovering = false
    afterRecoveryCompleted(lastSequenceNr, state)
  }
}
