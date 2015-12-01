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
package repositories

import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent._
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.json.BSONFormats._
import play.api.libs.json._
import models._
import models.BaseFormat._
import repositories.MongoDBCommandSet._
import org.openqa.selenium.support.FindAll
import play.api.Logger

trait JiraConfigRepository extends BaseRepository[JiraConfig, JiraConfigId] {

  def getJiraConfigurations(): Future[Seq[JiraConfig]]
}

class JiraConfigMongoRepository extends BaseReactiveMongoRepository[JiraConfig, JiraConfigId] with JiraConfigRepository {
  def coll = db.collection[JSONCollection]("JiraConfig")

  def getJiraConfigurations(): Future[Seq[JiraConfig]] = {
    find(Json.obj()) map { configs =>
      configs.map(_._1).toSeq
    }
  }
}