package scripts
import akka.actor.ActorSystem
import akka.contrib.persistence.mongodb.{Atom, Event, Legacy, RxMongoSerializers, RxMongoSerializersExtension}
import akka.contrib.persistence.mongodb.JournallingFieldNames._
import akka.contrib.persistence.mongodb.RxMongoSerializers._
import akka.persistence.PersistentRepr
import akka.serialization.Serialization
import models._
import play.api.Play.current
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.{Cursor, SerializationPack}
import reactivemongo.api.collections.GenericCollection
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.commands.UpdateWriteResult
import reactivemongo.bson.{BSONArray, BSONDocument, BSONObjectID}
import reactivemongo.play.json.collection.JSONCollection
import reactivemongo.play.json._
import repositories._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
object UpdateUserId {

  /**
    * Make sure you make a backup before executing this.
    *
    * Following steps need to be carried out:
    * - make sure there aren't any journal entries which have a different `_t` then `repr`
    * - make sure there aren't any documents within journal with the newUserId, otherwise
    *   - remove them manually e.g.: db.journal.remove({"pid":"x.z"})
    * - db.akka_persistence_realtime.drop()  => just to be sure as the oldUserId is in there as well
    */
  def update(
    oldUserId: String,
    newUserId: String
  )(implicit serialization: Serialization, system: ActorSystem) {

    lazy val reactiveMongoApi = current.injector.instanceOf[ReactiveMongoApi]
    def db = reactiveMongoApi.database
    val rxMongoSerializers: RxMongoSerializers =RxMongoSerializersExtension(system)

    val journalCollection: Future[BSONCollection] = {
      val coll = db.map(_.collection[JSONCollection]("journal"))
      db.flatMap(d => coll.map(c => d.collection(c.name, c.failoverStrategy)))
    }

    def errorHandling(
      future: Future[UpdateWriteResult]
    ): Future[UpdateWriteResult] = {
      future
        .map(x => if (!x.ok) println(s"errrrrRRRRRRRRRRRRRRRRRRR: ${x.errmsg}"))
        .onFailure {
          case t =>
            print(
              "Future errrrrRRRRRRRRRRRRRRRRRRR following the stacktrace: " + t
                .printStackTrace()
            )
        }
      future
    }

    def update[P <: SerializationPack with Singleton, S, U](
      coll: GenericCollection[P],
      selector: S,
      modifier: U
    )(implicit selectorWriter: coll.pack.Writer[S],
      updateWriter: coll.pack.Writer[U]) =
      errorHandling(
        coll.update(selector, modifier, upsert = false, multi = true)
      )

    def updateUserId(repos: BaseRepository[_, _]*): Seq[Future[UpdateWriteResult]] =
      repos.map(repo => {
        println(s"Updating userId on ${repo.getClass.getSimpleName}")
        repo.coll.flatMap(coll => {
          update(
            coll,
            Json.obj("userId" -> oldUserId),
            Json.obj("$set" -> Json.obj("userId" -> newUserId))
          )
        })
      })

    def updateId(repos: BaseRepository[_, _]*) =
      repos.map(repo => {
        println(s"Updating id on ${repo.getClass.getSimpleName}")
        repo.coll.flatMap(coll => {
          update(
            coll,
            Json.obj("id" -> oldUserId),
            Json.obj("$set" -> Json.obj("id" -> newUserId))
          )
        })
      })

    def copyPersistentEvent(persistentRepr: PersistentRepr) ={
      val userId = UserId(newUserId)
      persistentRepr.payload.asInstanceOf[PersistetEvent] match {

        case UserLoggedIn(_)                => UserLoggedIn(userId)
        case UserTimeBookingInitialized(_)  => UserTimeBookingInitialized(userId)
        case UserLoggedOut(_)               => UserLoggedOut(userId)

        case UserTimeBookingStarted(booking)            => UserTimeBookingStarted(booking.copy(userId = userId))
        case UserTimeBookingStopped(booking)            => UserTimeBookingStopped(booking.copy(userId = userId))
        case UserTimeBookingRemoved(booking)            => UserTimeBookingRemoved(booking.copy(userId = userId))
        case UserTimeBookingAdded(booking)              => UserTimeBookingAdded(booking.copy(userId = userId))
        case UserTimeBookingEdited(booking, start, end) => UserTimeBookingEdited(booking.copy(userId = userId), start, end)

        case UserTimeBookingPaused(bookingId, time) =>
          UserTimeBookingPaused(bookingId, time)
        case UserTimeBookingStartTimeChanged(bookingId, fromStart, toStart) =>
          UserTimeBookingStartTimeChanged(bookingId, fromStart, toStart)
        case UndefinedEvent => UndefinedEvent
      }
    }

    def copyJournal(doc: BSONDocument): BSONDocument = {
      val events = doc.as[BSONArray](EVENTS).values.collect {
        case d: BSONDocument =>
          val event = rxMongoSerializers.JournalDeserializer.deserializeDocument(d)
          val persistentRepr = event.payload match {
            case l: Legacy =>
              serialization
                .serializerFor(classOf[PersistentRepr])
                .fromBinary(l.bytes)
                .asInstanceOf[PersistentRepr]
            case _ =>
              throw new IllegalStateException("payload not of type Legacy")
          }

          val newPayload = copyPersistentEvent(persistentRepr)
          event.copy(
            pid = newUserId,
            writerUuid = Some(newUserId),
            payload = Legacy(persistentRepr.withPayload(newPayload), Set.empty[String]) // TODO tags
          )
      }
      val firstEvent = events.head
      val newJournal = rxMongoSerializers.JournalSerializer.serializeAtom(
        new Atom(firstEvent.pid, firstEvent.sn, firstEvent.sn, events)
      )
      newJournal
    }
    def updatePids(colls: BSONCollection*): Future[Seq[Future[Traversable[UpdateWriteResult]]]] = {
      Future.sequence(colls.map(coll => {
        println(s"Updating ${coll.name}")

        coll
          .find(Json.obj(PROCESSOR_ID -> oldUserId))
          .cursor[BSONDocument]()
          .collect[Traversable](Integer.MAX_VALUE, Cursor.FailOnError())
          .map(
            x =>
              Future.sequence(x.map { doc =>
                val newJournal: BSONDocument = copyJournal(doc)
                val id = doc.as[BSONObjectID]("_id")
                errorHandling(journalCollection.flatMap(_.update(Json.obj("_id" -> id), newJournal)))
              })
          )
      }))
    }

    println(s"updating $oldUserId to $newUserId")

    val updatedIds: Seq[Future[UpdateWriteResult]] =
      updateId(new UserMongoRepository(), new UserFavoritesMongoRepository())

    val updatedUserIds: Seq[Future[UpdateWriteResult]] = updateUserId(
      new BookingByCategoryMongoRepository(),
      new BookingByProjectMongoRepository(),
      new BookingByTagMongoRepository(),
      new BookingHistoryMongoRepository()
    )

    println("waiting for updates to finish")
    Await.result(
    for {
          coll <- journalCollection
          updatedPids <- updatePids(coll)
          results <- Future.sequence(updatedIds ++ updatedUserIds ++ updatedPids)
      } yield results, 30 seconds
    )
    println("updates done")
  }
}
