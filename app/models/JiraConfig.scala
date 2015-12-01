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

import reactivemongo.bson.BSONObjectID
import java.net.URL
import models.BaseFormat._
import play.api.libs.json._

case class JiraConfigId(value: BSONObjectID = BSONObjectID.generate) extends BaseBSONObjectId
case class ProjectMapping(projectId: ProjectId, jiraProjectKey:String)
case class JiraConfig(id: JiraConfigId, baseUrl: URL, consumerKey:String, privateKey:String, accessToken:String, 
    projects: Seq[ProjectMapping]) extends BaseEntity[JiraConfigId]

object JiraConfigId {
  implicit val idFormat: Format[JiraConfigId] = BaseFormat.idformat[JiraConfigId](JiraConfigId.apply _)
}

object ProjectMapping {
  implicit val mappingFormat: Format[ProjectMapping] = Json.format[ProjectMapping]
}
    
object JiraConfig {
  implicit val jiraConfigFormat: Format[JiraConfig] = Json.format[JiraConfig]
}