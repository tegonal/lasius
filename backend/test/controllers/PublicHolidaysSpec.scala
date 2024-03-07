/*
 *
 * Lasius - Open source time tracker for teams
 * Copyright (c) Tegonal Genossenschaft (https://tegonal.com)
 *
 * This file is part of Lasius.
 *
 * Lasius is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * Lasius is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Lasius. If not, see <https://www.gnu.org/licenses/>.
 */

package controllers

import core.{SystemServices, TestApplication}
import models._
import mongo.EmbedMongo
import org.joda.time.LocalDate
import org.specs2.mock.Mockito
import org.specs2.mock.mockito.MockitoMatchers
import play.api.mvc._
import play.api.test._

import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class PublicHolidaysSpec
    extends PlaySpecification
    with Mockito
    with Results
    with MockitoMatchers
    with EmbedMongo
    with TestApplication {

  "create public holiday entry" should {
    "badrequest creating duplicate entry (same date) in the same organisation" in new WithOrganisationsControllerMock {
      val date = LocalDate.now()
      withDBSession() { implicit dbSession =>
        controller.publicHolidayRepository.upsert(
          PublicHoliday(
            id = PublicHolidayId(),
            organisationReference = controller.organisation.reference,
            date = date,
            name = "Holiday1",
            year = date.getYear
          )
        )
      }.awaitResult()

      val request: FakeRequest[CreatePublicHoliday] = FakeRequest()
        .withBody(
          CreatePublicHoliday(
            date = date,
            name = ""
          ))
      val result: Future[Result] =
        controller.createPublicHoliday(controller.organisationId)(request)

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must equalTo(
        s"Cannot create duplicate public holidays entry with same date $date in organisation ${controller.organisation.key}")
    }

    "successful, even if for the same date another entry in another org exists" in new WithOrganisationsControllerMock {
      val org2 = Organisation(
        id = OrganisationId(),
        key = "Some key",
        `private` = false,
        active = true,
        createdBy = controller.userReference,
        deactivatedBy = None
      )

      val date = LocalDate.now()

      withDBSession() { implicit dbSession =>
        controller.organisationRepository.upsert(org2).awaitResult()

        controller.publicHolidayRepository.upsert(
          PublicHoliday(
            id = PublicHolidayId(),
            organisationReference = org2.reference,
            date = date,
            name = "Holiday1",
            year = date.getYear
          )
        )
      }.awaitResult()

      val request: FakeRequest[CreatePublicHoliday] = FakeRequest()
        .withBody(
          CreatePublicHoliday(
            date = date,
            name = "Holiday1"
          ))
      val result: Future[Result] =
        controller.createPublicHoliday(controller.organisationId)(request)

      status(result) must equalTo(CREATED)
      val payload = contentAsJson(result).as[PublicHoliday]
      payload.date === date
    }
  }

  "update public holiday entry" should {

    "badrequest if public holidays is not assigned to the provided organisation" in new WithOrganisationsControllerMock {
      val org2 = Organisation(
        id = OrganisationId(),
        key = "Some key",
        `private` = false,
        active = true,
        createdBy = controller.userReference,
        deactivatedBy = None
      )

      val date            = LocalDate.now()
      val publicHolidayId = PublicHolidayId()
      withDBSession() { implicit dbSession =>
        controller.organisationRepository.upsert(org2).awaitResult()

        controller.publicHolidayRepository.upsert(
          PublicHoliday(
            id = publicHolidayId,
            organisationReference = org2.reference,
            date = date,
            name = "Holiday1",
            year = date.getYear
          )
        )
      }.awaitResult()

      val request: FakeRequest[UpdatePublicHoliday] = FakeRequest()
        .withBody(
          UpdatePublicHoliday(
            name = "Holiday2"
          ))
      val result: Future[Result] =
        controller.updatePublicHoliday(controller.organisationId,
                                       publicHolidayId)(request)

      status(result) must equalTo(BAD_REQUEST)
    }

    "forbidden if user is not assigned to organisation" in new WithOrganisationsControllerMock {
      val org2 = Organisation(
        id = OrganisationId(),
        key = "Some key",
        `private` = false,
        active = true,
        createdBy = controller.userReference,
        deactivatedBy = None
      )

      val date            = LocalDate.now()
      val publicHolidayId = PublicHolidayId()
      withDBSession() { implicit dbSession =>
        controller.organisationRepository.upsert(org2).awaitResult()

        controller.publicHolidayRepository.upsert(
          PublicHoliday(
            id = publicHolidayId,
            organisationReference = org2.reference,
            date = date,
            name = "Holiday1",
            year = date.getYear
          )
        )
      }.awaitResult()

      val request: FakeRequest[UpdatePublicHoliday] = FakeRequest()
        .withBody(
          UpdatePublicHoliday(
            name = "Holiday2"
          ))
      val result: Future[Result] =
        controller.updatePublicHoliday(org2.id, publicHolidayId)(request)

      status(result) must equalTo(FORBIDDEN)
    }

    "successful" in new WithOrganisationsControllerMock {
      val publicHolidayId = PublicHolidayId()
      withDBSession() { implicit dbSession =>
        controller.publicHolidayRepository.upsert(
          PublicHoliday(
            id = publicHolidayId,
            organisationReference = controller.organisation.reference,
            date = LocalDate.now(),
            name = "Holiday1",
            year = LocalDate.now().getYear
          )
        )
      }.awaitResult()

      val request: FakeRequest[UpdatePublicHoliday] = FakeRequest()
        .withBody(
          UpdatePublicHoliday(
            name = "Holiday2"
          ))
      val result: Future[Result] =
        controller.updatePublicHoliday(controller.organisationId,
                                       publicHolidayId)(request)

      status(result) must equalTo(OK)
      val payload = contentAsJson(result).as[PublicHoliday]
      payload.name === "Holiday2"
    }
  }

  "delete a public holiday entry" should {
    "forbidden if user is not assigned to organisation" in new WithOrganisationsControllerMock {
      val org2 = Organisation(
        id = OrganisationId(),
        key = "Some key",
        `private` = false,
        active = true,
        createdBy = controller.userReference,
        deactivatedBy = None
      )

      val date            = LocalDate.now()
      val publicHolidayId = PublicHolidayId()
      withDBSession() { implicit dbSession =>
        controller.organisationRepository.upsert(org2).awaitResult()

        controller.publicHolidayRepository.upsert(
          PublicHoliday(
            id = publicHolidayId,
            organisationReference = org2.reference,
            date = date,
            name = "Holiday1",
            year = date.getYear
          )
        )
      }.awaitResult()

      val request: FakeRequest[Unit] = FakeRequest().withBody(())
      val result: Future[Result] =
        controller.deletePublicHoliday(org2.id, publicHolidayId)(request)

      status(result) must equalTo(FORBIDDEN)
    }
  }

  trait WithOrganisationsControllerMock extends WithTestApplication {
    implicit val executionContext: ExecutionContext = inject[ExecutionContext]
    val systemServices: SystemServices              = inject[SystemServices]
    val authConfig: AuthConfig                      = inject[AuthConfig]
    val controller: OrganisationsControllerMock =
      controllers.OrganisationsControllerMock(systemServices,
                                              authConfig,
                                              reactiveMongoApi)
  }
}
