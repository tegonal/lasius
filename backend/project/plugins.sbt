resolvers += "Typesafe repository".at(
  "https://repo.typesafe.com/typesafe/releases/")

// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.9.0")

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.6")

addSbtPlugin("com.dwijnand" % "sbt-dynver" % "4.1.1")

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.11.0")

addSbtPlugin("io.github.play-swagger" % "sbt-play-swagger" % "1.4.4")

addSbtPlugin("de.heikoseeberger" % "sbt-header" % "5.7.0")

addDependencyTreePlugin
