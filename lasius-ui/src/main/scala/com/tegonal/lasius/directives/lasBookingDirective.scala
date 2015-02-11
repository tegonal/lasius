package com.tegonal.lasius.directives

import scala.scalajs.js.annotation.JSExport
import com.greencatsoft.angularjs.ElementDirective
import com.greencatsoft.angularjs.TemplatedDirective
import com.greencatsoft.angularjs.IsolatedScope
import com.greencatsoft.angularjs.inject

@JSExport
object LasBookingDirective extends ElementDirective with TemplatedDirective with IsolatedScope {

  override val name = "lasBooking"

  override val templateUrl = "assets/templates/las-booking-tmpl.html"
    
  @inject
  var bookingService: Location = _

  bindings ++= Seq()

  @JSExport
  def onEditStart(scope: ScopeType) {
    scope.editing = true
    scope.title = scope.todo.title
  }

  @JSExport
  def onEditEnd(scope: ScopeType) {
    scope.editing = false
    scope.todo.title = scope.title

    scope.fireOnChange()
  }

  @JSExport
  def onEditCancel(scope: ScopeType) {
    scope.editing = false
    scope.title = scope.todo.title
  }

  class ScopeType extends Scope {

    var title: String = ???

    var editing: Boolean = ???

    def todo: Task = ???

    def fireOnRemove(): Unit = ???

    def fireOnChange(): Unit = ???
  }
}