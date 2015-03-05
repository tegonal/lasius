package repositories

trait BasicRepositoryComponent extends SecurityRepositoryComponent {
  val structureRepository: StructureRepository
}

trait MongoBasicRepositoryComponent extends BasicRepositoryComponent {
  val userRepository = new UserMongoRepository
  val structureRepository = new StructureMongoRepository
}