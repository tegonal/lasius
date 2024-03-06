package repositories

import com.tegonal.play.json.TypedId.BaseId
import core.DBSession
import models.BaseEntityWithOrgRelation
import models.OrganisationId.OrganisationReference
import play.api.libs.json.Json
import play.api.libs.json.Json.JsValueWrapper

import scala.concurrent.{ExecutionContext, Future}

trait BaseRepositoryWithOrgRef[T <: BaseEntityWithOrgRelation[ID],
                               ID <: BaseId[_]]
    extends BaseRepository[T, ID] {
  def findByOrganisationAndId(organisationReference: OrganisationReference,
                              id: ID)(implicit
      dbSession: DBSession,
      executionContext: ExecutionContext,
      fact: ID => JsValueWrapper): Future[Option[T]]
}

trait BaseReactiveMongoRepositoryWithOrgRef[T <: BaseEntityWithOrgRelation[ID],
                                            ID <: BaseId[_]]
    extends BaseReactiveMongoRepository[T, ID]
    with BaseRepositoryWithOrgRef[T, ID] {
  override def findByOrganisationAndId(
      organisationReference: OrganisationReference,
      id: ID)(implicit
      dbSession: DBSession,
      executionContext: ExecutionContext,
      fact: ID => JsValueWrapper): Future[Option[T]] = {
    findFirst(
      Json.obj("organisationReference.id" -> organisationReference.id,
               "id"                       -> fact(id))).map { result =>
      result.map(_._1)
    }
  }
}
