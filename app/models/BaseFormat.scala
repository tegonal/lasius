package models

import com.tegonal.play.json.TypedId.BaseId
import reactivemongo.bson.BSONObjectID

object BaseFormat {
  trait BaseBSONObjectId extends BaseId[BSONObjectID]
}