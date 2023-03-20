resolvers += "Typesafe repository".at(
  "https://repo.typesafe.com/typesafe/releases/")

// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.8.15")

// web plugins

addSbtPlugin("com.typesafe.sbt" % "sbt-coffeescript" % "1.0.2")

addSbtPlugin("com.typesafe.sbt" % "sbt-less" % "1.1.2")

addSbtPlugin("com.typesafe.sbt" % "sbt-jshint" % "1.0.6")

addSbtPlugin("com.typesafe.sbt" % "sbt-rjs" % "1.0.10")

addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.1.4")

addSbtPlugin("com.typesafe.sbt" % "sbt-mocha" % "1.1.2")

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.6")

addSbtPlugin("com.dwijnand" % "sbt-dynver" % "4.1.1")

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.11.0")

addSbtPlugin("com.iheart" % "sbt-play-swagger" % "0.10.6-PLAY2.8")

addSbtPlugin("de.heikoseeberger" % "sbt-header" % "5.7.0")

addDependencyTreePlugin
