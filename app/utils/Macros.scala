package utils

import scala.reflect.macros.whitebox.Context
import language.experimental.macros

object Macros {
  def inferImplicitsImpl[A: c.WeakTypeTag](c: Context): c.Expr[A] = {
    import c.universe._
    // gets type of input type
    val theType = weakTypeOf[A]

    // builds expected implicit MyTypeClass[A]
    val neededImplicitType = appliedType(weakTypeOf[A].typeConstructor, theType :: Nil)

    // searches an implicit in scope
    val neededImplicit = c.inferImplicitValue(neededImplicitType)

    neededImplicit match {
      case EmptyTree => c.abort(c.enclosingPosition, s"No implicit ${theType} available.")
      case impl => c.Expr[A](impl)
    }
  }

  def inferImplicits[A]: A = macro inferImplicitsImpl[A]

}