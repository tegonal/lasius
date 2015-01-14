package repositories

trait BasicRepositoryComponent {
  val userRepository: UserRepository
  val structureRepository: StructureRepository
}

trait MongoBasicRepositoryComponent extends BasicRepositoryComponent {
  val userRepository = new UserMongoRepository
  val structureRepository = new StructureMongoRepository
}