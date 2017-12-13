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
package core

import play.api.Logger
import models._
import play.api.libs.concurrent.Execution.Implicits._
import repositories._
import org.mindrot.jbcrypt.BCrypt
import java.net.URL

object InitialData extends MongoBasicRepositoryComponent {
  def init() = {
    Logger.debug("Initialize user data...")
    userRepository.coll.drop map { r =>
      initializeUsers()
    } recoverWith {
      case t => initializeUsers()
    }
  }

  def initializeUsers() = {
    val team = Team(TeamId(), "Team1")

    val passwordHash = BCrypt.hashpw("noob", BCrypt.gensalt())
    userRepository.insert(User(UserId("noob"), "noob@test.com", passwordHash, "Demo", "User", true, FreeUser, Seq(team)))

    val passwordHash2 = BCrypt.hashpw("demo", BCrypt.gensalt())
    userRepository.insert(User(UserId("demo"), "demo@test.com", passwordHash2, "Demo", "User2", true, FreeUser, Seq(team)))
    
  }
}
