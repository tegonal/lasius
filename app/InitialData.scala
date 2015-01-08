import dao.MongoBasicDAOComponent
import play.api.Logger
import models._
import play.api.libs.concurrent.Execution.Implicits._

object InitialData extends MongoBasicDAOComponent {
  def init() = {
    Logger.debug("Initialize user data...")
    userDAO.coll.drop map { r =>
      initializeUsers()
    } recoverWith {
      case t => initializeUsers()
    }

    Logger.debug("Initialize project data...")
    structureDAO.coll.drop map { r =>
      initializeStructure()
    } recoverWith {
      case t => initializeStructure()
    }
  }

  def initializeUsers() = {
    userDAO.insert(User(UserId("noob"), "Demo", "User", true))
  }

  def initializeStructure() = {
    structureDAO.insert(Category(CategoryId("Projects"),
      Seq(Project(ProjectId("Lasius"),
        Seq(Tag(TagId("LS-1")),
          Tag(TagId("LS-2")))),
        Project(ProjectId("Sirius"),
          Seq(Tag(TagId("SI-1")),
            Tag(TagId("SI-2")))),
        Project(ProjectId("Apus"),
          Seq(Tag(TagId("AP-1")),
            Tag(TagId("AP-2")))))))
    structureDAO.insert(Category(CategoryId("Administration"),
      Seq(Project(ProjectId("Marketing"),
        Seq(Tag(TagId("Sales")),
          Tag(TagId("Cold Aquisition")))),
        Project(ProjectId("KnowHow"), Nil),
        Project(ProjectId("Others"), Nil))))
  }
}