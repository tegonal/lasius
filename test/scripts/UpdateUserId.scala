package scripts
import akka.actor.ActorSystem
import akka.contrib.persistence.mongodb.{Atom, Legacy}
import akka.contrib.persistence.mongodb.JournallingFieldNames._
import akka.contrib.persistence.mongodb.RxMongoSerializers._
import akka.persistence.PersistentRepr
import akka.serialization.Serialization
import models._
import play.api.Play.current
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json._
import play.modules.reactivemongo.json.collection.{JSONCollection, _}
import reactivemongo.api.SerializationPack
import reactivemongo.api.collections.GenericCollection
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.commands.UpdateWriteResult
import reactivemongo.bson.{BSONArray, BSONDocument, BSONObjectID}
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
    def db = reactiveMongoApi.db

    val journalCollection: BSONCollection = {
      val coll = db.collection[JSONCollection]("journal")
      db.collection(coll.name, coll.failoverStrategy)
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

    def updateUserId(repos: BaseRepository[_, _]*) =
      repos.map(repo => {
        println(s"Updating userId on ${repo.getClass.getSimpleName}")
        update(
          repo.coll,
          Json.obj("userId" -> oldUserId),
          Json.obj("$set" -> Json.obj("userId" -> newUserId))
        )

      })

    def updateId(repos: BaseRepository[_, _]*) =
      repos.map(repo => {
        println(s"Updating id on ${repo.getClass.getSimpleName}")
        update(
          repo.coll,
          Json.obj("id" -> oldUserId),
          Json.obj("$set" -> Json.obj("id" -> newUserId))
        )
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

    def copyJournal(doc: BSONDocument) = {


      val events = doc.as[BSONArray](EVENTS).values.collect {
        case d: BSONDocument =>
          val event = JournalDeserializer.deserializeDocument(d)
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
            payload = Legacy(persistentRepr.withPayload(newPayload))
          )
      }
      val firstEvent = events.head
      val newJournal = JournalSerializer.serializeAtom(
        new Atom(firstEvent.pid, firstEvent.sn, firstEvent.sn, events)
      )
      newJournal
    }
    def updatePids(colls: BSONCollection*) = {

      colls.map(coll => {
        println(s"Updating ${coll.name}")

        coll
          .find(Json.obj(PROCESSOR_ID -> oldUserId))
          .cursor[BSONDocument]()
          .collect[Traversable]()
          .flatMap(
            x =>
              Future.sequence(x.map { doc =>
                val newJournal: BSONDocument = copyJournal(doc)
                val id = doc.as[BSONObjectID]("_id")
                errorHandling(journalCollection.update(Json.obj("_id" -> id), newJournal))
              })
          )
      })
    }

    println(s"updating $oldUserId to $newUserId")

    val updatedIds =
      updateId(new UserMongoRepository(), new UserFavoritesMongoRepository())

    val updatedUserIds = updateUserId(
      new BookingByCategoryMongoRepository(),
      new BookingByProjectMongoRepository(),
      new BookingByTagMongoRepository(),
      new BookingHistoryMongoRepository()
    )

    val updatedPids = updatePids(journalCollection)

    println("waiting for updates to finish")
    Await.result(
      Future.sequence(updatedIds ++ updatedUserIds ++ updatedPids),
      30 seconds
    )
    println("updates done")
  }
}
