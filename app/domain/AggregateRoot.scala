package domain

import akka.persistence._
import akka.actor._

object AggregateRoot {
  trait State
  trait Command
  trait Event

  case object GetState extends Command
  case class Initialize(state: State) extends Command

  case object Removed extends State
  case object Created extends State
  case object Uninitialized extends State
}

trait AggregateRoot extends PersistentActor with ActorLogging {

  import AggregateRoot._
  var state: State

  def updateState(evt: Event): Unit
  def restoreFromSnapshot(metadata: SnapshotMetadata, state: State)

  protected def afterEventPersisted(evt: Event): Unit = {
    updateState(evt)
    publish(evt)
    log.debug(s"afterEventPersisted:send back state:$state")
    sender ! state
  }

  private def publish(event: Event) =
    context.system.eventStream.publish(event)

  override val receiveRecover: Receive = {
    case evt: Event =>
      updateState(evt)
    case SnapshotOffer(metadata, state: State) =>
      restoreFromSnapshot(metadata, state)
      log.debug("recovering aggregate from snapshot")
  }
}