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
import com.tegonal.play.json._
import play.api.libs.json._
import com.tegonal.play.json.TypedId._
import julienrf.variants.Variants
import java.net.URI
import models.BaseFormats._

case class TagId(value: String) extends StringBaseId
case class ProjectId(value: String) extends StringBaseId
case class CategoryId(value: String) extends StringBaseId

object CategoryId {
  implicit val idFormat: Format[CategoryId] = Json.idformat[CategoryId](CategoryId.apply _)
}
object ProjectId {
  implicit val idFormat: Format[ProjectId] = Json.idformat[ProjectId](ProjectId.apply _)
}
object TagId {
  implicit val idFormat: Format[TagId] = Json.idformat[TagId](TagId.apply _)
}

sealed trait BaseTag {
  val id: TagId
}
case class Tag(id: TagId) extends BaseEntity[TagId] with BaseTag
case class JiraIssueTag(id: TagId, baseUrl: String, summary:Option[String], 
    url:URI, projectKey:String) extends BaseEntity[TagId] with BaseTag 
case class JiraVersionTag(id: TagId, configId: JiraConfigId, projectKey:String) extends BaseEntity[TagId] with BaseTag

case class Project(id: ProjectId, tags: Seq[Tag]) extends BaseEntity[ProjectId]
case class Category(id: CategoryId, projects: Seq[Project]) extends BaseEntity[CategoryId]

object JiraIsseTag{ 
  implicit val issueTagFormat: Format[JiraIssueTag]  = Json.format[JiraIssueTag]
}
object JiraVersionTag{ 
  implicit val issueVersionFormat: Format[JiraVersionTag]  = Json.format[JiraVersionTag]
}
object Tag{
  implicit val tagFormat: Format[Tag]  = Json.format[Tag]
}
object BaseTag {
  implicit val baseTagFormat: Format[BaseTag]  = Variants.format[BaseTag]("type")  
}

object Project {
  implicit val projectFormat: Format[Project] = Json.format[Project]
}

object Category {
  implicit val categoryFormat: Format[Category] = Json.format[Category]
}