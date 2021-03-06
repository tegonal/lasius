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
package domain

import akka.persistence._
import akka.actor._
import play.api.libs.json.Format
import models.PersistetEvent

object AggregateRoot {
  trait State
  trait Command

  case object GetState extends Command
  case class Initialize(state: State) extends Command

  case object Removed extends State
  case object Created extends State
  case object Uninitialized extends State
}

trait AggregateRoot extends PersistentActor with ActorLogging {

  import AggregateRoot._
  var state: State

  def updateState(evt: PersistetEvent): Unit
  def restoreFromSnapshot(metadata: SnapshotMetadata, state: State)

  protected def afterEventPersisted(evt: PersistetEvent): Unit = {
    updateState(evt)
    publish(evt)
    log.debug(s"afterEventPersisted:send back state:$state")
    sender ! state
  }

  private def publish(event: PersistetEvent) =
    context.system.eventStream.publish(event)

  override val receiveRecover: Receive = {
    case evt: PersistetEvent =>
      log.debug(s"receiveRecover:$evt")
      updateState(evt)
    case SnapshotOffer(metadata, state: State) =>
      restoreFromSnapshot(metadata, state)
      log.debug("recovering aggregate from snapshot")
  }
}