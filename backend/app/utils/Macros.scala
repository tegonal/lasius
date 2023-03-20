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

package utils

import scala.language.experimental.macros
import scala.reflect.macros.whitebox

object Macros {
  def inferImplicitsImpl[A: c.WeakTypeTag](c: whitebox.Context): c.Expr[A] = {
    import c.universe._
    // gets type of input type
    val theType = weakTypeOf[A]

    // builds expected implicit MyTypeClass[A]
    val neededImplicitType =
      appliedType(weakTypeOf[A].typeConstructor, theType :: Nil)

    // searches an implicit in scope
    val neededImplicit = c.inferImplicitValue(neededImplicitType)

    neededImplicit match {
      case EmptyTree =>
        c.abort(c.enclosingPosition, s"No implicit $theType available.")
      case impl => c.Expr[A](impl)
    }
  }

  def inferImplicits[A]: A = macro inferImplicitsImpl[A]

}
