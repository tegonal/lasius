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

package models

import org.specs2.mutable._
import play.api.libs.json
import play.api.libs.json.JsSuccess

class JsonEnumSpec extends Specification {
  "Granularity format" should {
    "be able to parse string values" in {
      Granularity.format.reads(json.JsString("Day")) === JsSuccess(Day)
      Granularity.format.reads(json.JsString("Week")) === JsSuccess(Week)
      Granularity.format.reads(json.JsString("Month")) === JsSuccess(Month)
      Granularity.format.reads(json.JsString("Year")) === JsSuccess(Year)
      Granularity.format.reads(json.JsString("All")) === JsSuccess(All)
    }

    "be able to write correct string values" in {
      Granularity.format.writes(Day) === json.JsString("Day")
      Granularity.format.writes(Week) === json.JsString("Week")
      Granularity.format.writes(Month) === json.JsString("Month")
      Granularity.format.writes(Year) === json.JsString("Year")
      Granularity.format.writes(All) === json.JsString("All")
    }
  }

  "UserRole format" should {
    "be able to parse string values" in {
      UserRole.format.reads(json.JsString("FreeUser")) === JsSuccess(FreeUser)
      UserRole.format.reads(json.JsString("Administrator")) === JsSuccess(
        Administrator)
    }

    "be able to write correct string values" in {
      UserRole.format.writes(FreeUser) === json.JsString("FreeUser")
      UserRole.format.writes(Administrator) === json.JsString("Administrator")
    }
  }

  "ProjectRole format" should {
    "be able to parse string values" in {
      ProjectRole.format.reads(json.JsString("ProjectMember")) === JsSuccess(
        ProjectMember)
      ProjectRole.format.reads(
        json.JsString("ProjectAdministrator")) === JsSuccess(
        ProjectAdministrator)
    }

    "be able to write correct string values" in {
      ProjectRole.format.writes(ProjectMember) === json.JsString(
        "ProjectMember")
      ProjectRole.format.writes(ProjectAdministrator) === json.JsString(
        "ProjectAdministrator")
    }
  }

  "InvitationOutcomeStatus format" should {
    "be able to parse string values" in {
      InvitationOutcomeStatus.format.reads(
        json.JsString("InvitationAccepted")) === JsSuccess(InvitationAccepted)
      InvitationOutcomeStatus.format.reads(
        json.JsString("InvitationDeclined")) === JsSuccess(InvitationDeclined)
    }

    "be able to write correct string values" in {
      InvitationOutcomeStatus.format.writes(InvitationAccepted) === json
        .JsString("InvitationAccepted")
      InvitationOutcomeStatus.format.writes(InvitationDeclined) === json
        .JsString("InvitationDeclined")
    }
  }

  "InvitationStatus format" should {
    "be able to parse string values" in {
      InvitationStatus.format.reads(
        json.JsString("UnregisteredUser")) === JsSuccess(UnregisteredUser)
      InvitationStatus.format.reads(
        json.JsString("InvitationOk")) === JsSuccess(InvitationOk)
    }

    "be able to write correct string values" in {
      InvitationStatus.format.writes(UnregisteredUser) === json
        .JsString("UnregisteredUser")
      InvitationStatus.format.writes(InvitationOk) === json
        .JsString("InvitationOk")
    }
  }

  "UserRole format" should {
    "be able to parse string values" in {
      UserRole.format.reads(json.JsString("FreeUser")) === JsSuccess(FreeUser)
      UserRole.format.reads(json.JsString("Administrator")) === JsSuccess(
        Administrator)
    }

    "be able to write correct string values" in {
      UserRole.format.writes(FreeUser) === json.JsString("FreeUser")
      UserRole.format.writes(Administrator) === json.JsString("Administrator")
    }
  }
}
