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

package core

import core.Validation.ValidationFailedException
import helpers.FutureHelper
import org.joda.time.DateTime
import play.api.mvc.Results.{Ok, Status}

import java.util.regex.Pattern
import scala.concurrent.Future

trait Validation extends FutureHelper {

  protected def success(): Future[Status] = Future.successful(Ok)

  protected def failed(errorMsg: String): Future[Status] =
    Future.failed(ValidationFailedException(errorMsg))

  protected def failIfNot(predicate: Boolean, errorMsg: String): Future[Unit] =
    failIf(!predicate, errorMsg)

  protected def failIf(predicate: Boolean, errorMsg: String): Future[Unit] = {
    if (predicate)
      Future.failed(ValidationFailedException(errorMsg))
    else
      Future.successful(())
  }

  protected def validate(predicate: Boolean,
                         errorMsg: => String): Future[Status] = {
    if (!predicate) {
      failed(errorMsg)
    } else {
      success()
    }
  }

  protected def validateStartBeforeEnd(start: DateTime,
                                       end: DateTime): Future[Status] =
    validate(start.isBefore(end) || start == end,
             s"Start date needs to be before end date $start <= $end")

  protected def validateEmail(email: String): Future[Status] =
    validate(Validation.emailPattern.matcher(email).matches(),
             s"Not a valid email address '$email'")

  /** Password Policy:
    *   - at least 8 characters long
    *   - one upper case letter
    *   - one lower case letter
    *   - one number
    *
    * @param password
    * @return
    */
  protected def validatePasswordPolicy(password: String): Future[Status] =
    validate(password.matches("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).{8,}$"),
             "password policy not satisfied")

  protected def validateNonBlankString(field: String,
                                       value: String): Future[Status] =
    validate(!value.isBlank, s"expected non-blank String for field '$field'")
}

object Validation {
  // taken from https://github.com/angular/angular/blob/fc64fa8e1af9e0bbab40d1b441743744a40c5581/packages/forms/src/validators.ts#L98
  // covers less cases than the one we had previously but is in sync with the client validation
  // see EmailTests for cases which would be valid/invalid but are not detected
  val emailPattern: Pattern = Pattern.compile(
    """^(?=.{1,254}$)(?=.{1,64}@)[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+)*@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$"""
  )

  case class ValidationFailedException(msg: String) extends Exception(msg)
}
