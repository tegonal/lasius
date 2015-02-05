package models

import play.api.libs.json._

import play.api.mvc.WebSocket.FrameFormatter
import reactivemongo.bson.BSONObjectID
import julienrf.variants.Variants

sealed trait InEvent

case class HelloServer(client: String) extends InEvent

object InEvent {
  implicit val inEventFormat: Format[InEvent] = Variants.format[InEvent]("type")
}

sealed trait OutEvent
case object HelloClient extends OutEvent
case class UserLoggedOut(userId: String) extends OutEvent

object OutEvent {
  implicit val outEventFormat: Format[OutEvent] = Variants.format[OutEvent]("type")
}

object Events {
  implicit val inEventFrameFormatter = FrameFormatter.jsonFrame[InEvent]
  implicit val outEventFrameFormatter = FrameFormatter.jsonFrame[OutEvent]
}
