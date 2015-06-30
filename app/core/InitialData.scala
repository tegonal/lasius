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
    val structure = createStructure()
    val structure2 = createStructure2()

    val passwordHash = BCrypt.hashpw("noob", BCrypt.gensalt())
    userRepository.insert(User(UserId("noob"), "noob@test.com", passwordHash, "Demo", "User", true, FreeUser, Seq(team), structure))

    val passwordHash2 = BCrypt.hashpw("demo", BCrypt.gensalt())
    userRepository.insert(User(UserId("demo"), "demo@test.com", passwordHash2, "Demo", "User2", true, FreeUser, Seq(team), structure2))
  }

  def createStructure() = {
    Seq(Category(CategoryId("Projects"),
      Seq(Project(ProjectId("Lasius"),
        Seq(Tag(TagId("LS-1")),
          Tag(TagId("LS-2")))),
        Project(ProjectId("Sirius"),
          Seq(Tag(TagId("SI-1")),
            Tag(TagId("SI-2")))),
        Project(ProjectId("Apus"),
          Seq(Tag(TagId("AP-1")),
            Tag(TagId("AP-2")))))), Category(CategoryId("Administration"),
      Seq(Project(ProjectId("Marketing"),
        Seq(Tag(TagId("Sales")),
          Tag(TagId("Cold Aquisition")))),
        Project(ProjectId("KnowHow"), Nil),
        Project(ProjectId("Others"), Nil))))
  }

  def createStructure2() = {
    Seq(Category(CategoryId("Projects"),
      Seq(Project(ProjectId("Lasius"),
        Seq(Tag(TagId("LS-1")),
          Tag(TagId("LS-2")))),
        Project(ProjectId("MPI"),
          Seq(Tag(TagId("MPI-1")),
            Tag(TagId("MPI-2")))))),
      Category(CategoryId("Sales"),
        Seq(Project(ProjectId("Aquisition"),
          Seq(Tag(TagId("Div")))))))
  }
}
