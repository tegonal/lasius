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
import julienrf.variants.Variants
import play.api.libs.json._
import reactivemongo.bson._
import play.modules.reactivemongo.json.BSONFormats._
import play.api.libs.json.JsValue

object AggregateRoot {
  trait State
  trait Command
  trait Event
  
  case class EventMsg(`type`:String, evt:Event)
  
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
  
  val typeFactory: Event => String

  protected def afterEventPersisted(evt:Event): Unit = {
    updateState(evt)
    publish(evt)
    log.debug(s"afterEventPersisted:send back state:$state")
    sender ! state
  }
  
  protected def toJson(evt:Event): JsValue
  
  def fromJson(typeString:String, evt:JsValue):Option[Event]
  
  def fromBson(bson:BSONDocument):Option[Event] = 
    fromJson(BSONDocumentFormat.writes(bson).as[JsValue])
  
  def fromJson(json:JsValue):Option[Event] = {
    json \ "type" match {
     case JsString(typeString) => 
       json \ "event" match {
         case evt:JsValue => fromJson(typeString, evt)
         case _ => None
       }
     case _ => None
    }
  }
  
  protected def withEvent[R](bson:BSONDocument)(handler: Event => R): Unit = {
    fromBson(bson) map {evt =>
    	handler(evt)
    }
  }  
  
  protected def withEvent[R](json:JsValue)(handler: Event => R): Unit = {
    fromJson(json) map {evt =>
    	handler(evt)
    }
  }  
  
  protected def persist(evt: Event)(handler: Event=> Unit):Unit = {    
    BSONDocumentFormat.reads(toJson(evt)).map(doc => persist(doc)(withEvent(_)(handler)))    
  }
  
  private def publish(event: Event) =
    context.system.eventStream.publish(event)    

  override val receiveRecover: Receive = {
    case bson:BSONDocument => 
      withEvent(bson)(updateState)
    case json:JsValue => 
      withEvent(json)(updateState)
    case evt: Event =>
      updateState(evt)
    case SnapshotOffer(metadata, state: State) =>
      restoreFromSnapshot(metadata, state)
      log.debug("recovering aggregate from snapshot")
    case e => 
      log.warning(s"Received recover of unknown command:$e")
  }
}