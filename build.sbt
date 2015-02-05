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
  "org.julienrf" %% "play-json-variants" % "1.0.0",
  "com.typesafe.akka" %% "akka-persistence-experimental" % "2.3.8",
  "com.github.ironfish" %% "akka-persistence-mongo-casbah" % "0.7.5",
  cache,
  ws,
  // WebJars (i.e. client-side) dependencies
  "org.webjars" % "requirejs" % "2.1.14-1",
  "org.webjars" % "underscorejs" % "1.6.0-3",
  "org.webjars" % "jquery" % "1.11.1",
  "org.webjars" % "bootstrap" % "3.1.1-2" exclude("org.webjars", "jquery"),
  "org.webjars" % "angularjs" % "1.2.18" exclude("org.webjars", "jquery"),
  "org.webjars" % "angular-ui-bootstrap" % "0.11.0-2",
  "org.webjars" % "angular-translate" % "2.4.0",
  "org.webjars" % "angular-translate-loader-static-files" % "2.4.0",
  "org.webjars" % "angular-dragdrop" % "1.0.3",
  "org.webjars" % "bootstrap-select" % "1.6.2-1",
  "org.webjars" % "angular-ui-select" % "0.8.3"
)

routesImport ++= Seq(
	"binders.Binders._",
	"play.api.i18n.Lang"
)
