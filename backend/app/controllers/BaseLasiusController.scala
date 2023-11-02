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

import core.{DBSupport, SystemServices}
import helpers.FutureHelper
import org.pac4j.core.profile.CommonProfile
import org.pac4j.play.scala.SecurityComponents
import play.api.Logging
import play.api.mvc.{AbstractController, BaseController, ControllerComponents}
import play.mvc.Controller

abstract class BaseLasiusController(
    override val controllerComponents: SecurityComponents)
    extends BaseController
    with Logging
    with Security[CommonProfile]
    with SecurityComponent
    with ControllerValidation
    with FutureHelper
    with DBSupport
    with org.pac4j.play.scala.Security[CommonProfile] {
  override val supportTransaction: Boolean = systemServices.supportTransaction

  val systemServices: SystemServices
}
