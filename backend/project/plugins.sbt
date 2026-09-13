//resolvers += "Typesafe repository".at(
//  "https://repo.typesafe.com/typesafe/releases/")

// The Play plugin
addSbtPlugin("org.playframework" % "sbt-plugin" % "3.0.10")

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.6")

addSbtPlugin("com.dwijnand" % "sbt-dynver" % "4.1.1")

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.13.1")

addSbtPlugin("io.github.play-swagger" % "sbt-play-swagger" % "3.1.7")

addSbtPlugin("de.heikoseeberger" % "sbt-header" % "5.10.0")

// Docker support
addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.11.7")

addDependencyTreePlugin
