import play.PlayImport.PlayKeys._

name := """lasius"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

resolvers += "Tegonal releases" at "https://github.com/tegonal/tegonal-mvn/raw/master/releases/"

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/releases"

libraryDependencies ++= Seq(
  "org.reactivemongo" %% "play2-reactivemongo" % "0.10.5.0.akka23",
  "com.tegonal" %% "play-json-typedid" % "1.0.1",
  "com.typesafe.akka" %% "akka-persistence-experimental" % "2.3.8",
  "com.github.ironfish" %% "akka-persistence-mongo-casbah" % "0.7.5",
  cache,
  ws
)

routesImport ++= Seq(
	"binders.Binders._",
	"play.api.i18n.Lang"
)
