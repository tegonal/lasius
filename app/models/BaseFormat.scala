package models

import com.tegonal.play.json.TypedId.BaseId
import reactivemongo.bson.BSONObjectID

trait BaseEntity[ID <: BaseId[_]] {
  val id: ID
}

object BaseFormat {
  trait BaseBSONObjectId extends BaseId[BSONObjectID]
}