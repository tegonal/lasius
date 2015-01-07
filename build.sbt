name := """hamster-online"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

resolvers += "Tegonal releases" at "https://github.com/tegonal/tegonal-mvn/raw/master/releases/"

libraryDependencies ++= Seq(
  "org.reactivemongo" %% "reactivemongo" % "0.10.5.0.akka23",
  "com.tegonal" %% "play-json-typedid" % "1.0.1",
  cache,
  ws
)
