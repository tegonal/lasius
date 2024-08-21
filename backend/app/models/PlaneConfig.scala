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

package models

import models.BaseFormat._
import play.api.libs.json._
import reactivemongo.api.bson.BSONObjectID

import java.net.URL

case class PlaneConfigId(value: BSONObjectID = BSONObjectID.generate())
    extends BaseBSONObjectId

case class PlaneSettings(checkFrequency: Long)

case class PlaneTagConfiguration(useLabels: Boolean,
                                 labelFilter: Set[String],
                                 useMilestone: Boolean = false,
                                 useTitle: Boolean = false)

case class PlaneProjectSettings(planeProjectId: String,
                                maxResults: Option[Int] = None,
                                params: Option[String] = None,
                                projectKeyPrefix: Option[String] = None,
                                tagConfiguration: PlaneTagConfiguration)

case class PlaneProjectMapping(projectId: ProjectId,
                               settings: PlaneProjectSettings)

case class PlaneAuth(consumerKey: String,
                     privateKey: String,
                     accessToken: String)

case class PlaneConfig(_id: PlaneConfigId,
                       name: String,
                       baseUrl: URL,
                       auth: PlaneAuth,
                       settings: PlaneSettings,
                       projects: Seq[PlaneProjectMapping])
    extends BaseEntity[PlaneConfigId] {
  val id: PlaneConfigId = _id
}

object PlaneConfigId {
  implicit val idFormat: Format[PlaneConfigId] =
    BaseFormat.idformat[PlaneConfigId](PlaneConfigId.apply)
}

object PlaneProjectMapping {
  implicit val mappingFormat: Format[PlaneProjectMapping] =
    Json.format[PlaneProjectMapping]
}

object PlaneSettings {
  implicit val PlaneSettingsFormat: Format[PlaneSettings] =
    Json.format[PlaneSettings]
}

object PlaneAuth {
  implicit val PlaneAuthFormat: Format[PlaneAuth] = Json.format[PlaneAuth]
}

object PlaneTagConfiguration {
  implicit val tagConfigFormat: Format[PlaneTagConfiguration] =
    Json.format[PlaneTagConfiguration]
}

object PlaneProjectSettings {
  implicit val settingsFormat: Format[PlaneProjectSettings] =
    Json.format[PlaneProjectSettings]
}

object PlaneConfig {
  implicit val PlaneConfigFormat: Format[PlaneConfig] =
    Json.format[PlaneConfig]
}
