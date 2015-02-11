package com.tegonal.lasius

import com.greencatsoft.angularjs._
import scala.scalajs.js._
import scala.scalajs.js.annotation.JSExport

@JSExport
object LasiusApp extends JSApp {

    override def main() {
    	val module = Angular.module("lasius", Seq("ngRoute", "ui.bootstrap"))
	
    }
}