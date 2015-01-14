import play.api.Logger
import models._
import play.api.libs.concurrent.Execution.Implicits._
import repositories._

object InitialData extends MongoBasicRepositoryComponent {
  def init() = {
    Logger.debug("Initialize user data...")
    userRepository.coll.drop map { r =>
      initializeUsers()
    } recoverWith {
      case t => initializeUsers()
    }

    Logger.debug("Initialize project data...")
    structureRepository.coll.drop map { r =>
      initializeStructure()
    } recoverWith {
      case t => initializeStructure()
    }
  }

  def initializeUsers() = {
    userRepository.insert(User(UserId("noob"), "Demo", "User", true))
  }

  def initializeStructure() = {
    structureRepository.insert(Category(CategoryId("Projects"),
      Seq(Project(ProjectId("Lasius"),
        Seq(Tag(TagId("LS-1")),
          Tag(TagId("LS-2")))),
        Project(ProjectId("Sirius"),
          Seq(Tag(TagId("SI-1")),
            Tag(TagId("SI-2")))),
        Project(ProjectId("Apus"),
          Seq(Tag(TagId("AP-1")),
            Tag(TagId("AP-2")))))))
    structureRepository.insert(Category(CategoryId("Administration"),
      Seq(Project(ProjectId("Marketing"),
        Seq(Tag(TagId("Sales")),
          Tag(TagId("Cold Aquisition")))),
        Project(ProjectId("KnowHow"), Nil),
        Project(ProjectId("Others"), Nil))))
  }
}