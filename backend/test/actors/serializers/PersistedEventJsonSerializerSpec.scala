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

package actors.serializers

import models.{
  Booking,
  PersistedEvent,
  ProjectId,
  SimpleTag,
  TagId,
  UserId,
  UserTimeBookingStarted
}
import org.specs2.matcher.Matchers
import org.specs2.mutable.Specification
import play.api.libs.json.{JsSuccess, Json}

import scala.annotation.nowarn

@nowarn("cat=deprecation")
class PersistedEventJsonSerializerSpec extends Specification with Matchers {
  "PersistedEventJsonSerializer" should {
    "be able to read old booking with TagId and convert it to a SimpleTag" in {
      val bookingAsJson =
        """{"booking":{"id":"68b8e127-2359-4d48-aaeb-7577c36cac12","start":1449577256223,"userId":"some.user","categoryId":"Projekte","projectId":"OpenOlitor","tags":["1b","OO-22"]},"type":"UserTimeBookingStarted"}"""

      // try read as json
      val fromJson = PersistedEvent.eventFormat.reads(Json.parse(bookingAsJson))
      val tags     = Set(SimpleTag(TagId("1b")), SimpleTag(TagId("OO-22")))
      fromJson must beLike {
        case JsSuccess(UserTimeBookingStarted(
                         Booking(_,
                                 _,
                                 None,
                                 "some.user",
                                 "Projekte",
                                 "OpenOlitor",
                                 t,
                                 None)),
                       _) if t == tags =>
          ok
      }

      // read through serializer
      val serializer = new PersistedEventJsonSerializer()
      val result =
        serializer.fromBinary(bookingAsJson.getBytes,
                              Some(classOf[UserTimeBookingStarted]))

      result must beAnInstanceOf[UserTimeBookingStarted]
      result.asInstanceOf[UserTimeBookingStarted].booking.tags must beEqualTo(
        Set(SimpleTag(TagId("1b")), SimpleTag(TagId("OO-22"))))
    }
  }
}
