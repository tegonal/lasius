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
package models

import java.net.URL

import models.BaseFormat._
import play.api.libs.json._
import reactivemongo.bson.BSONObjectID

case class JiraConfigId(value: BSONObjectID = BSONObjectID.generate) extends BaseBSONObjectId
case class JiraSettings(checkFrequency: Long)
case class ProjectSettings(jiraProjectKey:String, maxResults:Option[Int] = None, jql:Option[String]=None)
case class ProjectMapping(projectId: ProjectId, settings:ProjectSettings)
case class JiraAuth(consumerKey:String, 
    privateKey:String, 
    accessToken:String)
case class JiraConfig(id: JiraConfigId,
    name: String,
    baseUrl: URL, 
    auth: JiraAuth,
    settings: JiraSettings,
    projects: Seq[ProjectMapping]) extends BaseEntity[JiraConfigId]

object JiraConfigId {
  implicit val idFormat: Format[JiraConfigId] = BaseFormat.idformat[JiraConfigId](JiraConfigId.apply _)
}

object ProjectMapping {
  implicit val mappingFormat: Format[ProjectMapping] = Json.format[ProjectMapping]
}

object JiraSettings {
  implicit val jiraSettingsFormat: Format[JiraSettings] = Json.format[JiraSettings]
}

object JiraAuth {
  implicit val jiraAuthFormat: Format[JiraAuth] = Json.format[JiraAuth]
}

object ProjectSettings {
  implicit val settingsFormat: Format[ProjectSettings] = Json.format[ProjectSettings]
}

object JiraConfig {
  implicit val jiraConfigFormat: Format[JiraConfig] = Json.format[JiraConfig]
}