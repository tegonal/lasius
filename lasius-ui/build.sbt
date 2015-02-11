enablePlugins(ScalaJSPlugin)

name := "lasius-ui"

scalaVersion := "2.11.5" // or any other Scala version >= 2.10.2

libraryDependencies += "com.greencatsoft" %%% "scalajs-angular" % "0.3"

scalaJSStage in Global := FastOptStage