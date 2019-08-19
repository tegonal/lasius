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

import core.DefaultReactiveMongoApiAware
import models._

import concurrent.ExecutionContext.Implicits.global
import play.api.libs.json._
import play.api.Logger
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent._

trait JiraConfigRepository extends BaseRepository[JiraConfig, JiraConfigId] {

  def getJiraConfigurations(): Future[Seq[JiraConfig]]
}

class JiraConfigMongoRepository extends BaseReactiveMongoRepository[JiraConfig, JiraConfigId] with JiraConfigRepository with DefaultReactiveMongoApiAware {
  def coll = db.map(_.collection[JSONCollection]("JiraConfig"))

  def getJiraConfigurations(): Future[Seq[JiraConfig]] = {
    find(Json.obj()) map { configs =>
      Logger.debug(s"Loaded configs:$configs")
      configs.map(_._1).toSeq
    }
  }
}