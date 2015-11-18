import play.PlayImport.PlayKeys._

name := """lasius"""

version := "1.0-SNAPSHOT"

lazy val jiraScalaApi = ProjectRef(uri("git://github.com/toggm/play-scala-jira-api.git"), "root")

lazy val root = (project in file(".")).dependsOn(jiraScalaApi).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

resolvers += "Tegonal releases" at "https://github.com/tegonal/tegonal-mvn/raw/master/releases/"

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/releases"

resolvers += "dnvriend at bintray" at "http://dl.bintray.com/dnvriend/maven"

val akkaVs = "2.3.14"

libraryDependencies ++= Seq(
  "org.reactivemongo" %% "play2-reactivemongo" % "0.11.7.play24",
  "com.tegonal" %% "play-json-typedid" % "1.0.1",
  "org.julienrf" %% "play-json-variants" % "1.0.0",
  "com.typesafe.akka" %% "akka-persistence-experimental" % akkaVs,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVs,  
  "com.typesafe.akka" %% "akka-testkit" % akkaVs % "test", 
  "com.github.scullxbones" %% "akka-persistence-mongo-rxmongo" % "0.4.2", 
  //reativemongo based connector for persistent akka  
  "org.mindrot" % "jbcrypt" % "0.3m",
  "com.github.athieriot" %% "specs2-embedmongo" % "0.7.0" % "test",
  "de.flapdoodle.embed" % "de.flapdoodle.embed.mongo" % "1.50.0" % "test",
  "com.github.dnvriend" %% "akka-persistence-inmemory" % "1.1.5" % "test",
  //Akka monitoring  
  "org.aspectj" % "aspectjweaver" % "1.8.7",
  "io.kamon" %% "kamon-core" % "0.3.5",  
  "io.kamon" %% "kamon-statsd" % "0.3.5",
  "io.kamon" %% "kamon-play" % "0.3.5",
  "io.kamon" %% "kamon-system-metrics" % "0.3.5",
  "io.kamon" %% "kamon-log-reporter"   % "0.3.5",
  "com.typesafe.play.plugins" %% "play-statsd" % "2.3.0",
  cache,
  ws,
  specs2,
  // WebJars (i.e. client-side) dependencies
  "org.webjars" % "requirejs" % "2.1.20",
  "org.webjars" % "underscorejs" % "1.8.3",
  "org.webjars" % "jquery" % "2.1.4",
  "org.webjars" % "bootstrap" % "3.3.5" exclude("org.webjars", "jquery"),
  "org.webjars" % "angularjs" % "1.4.7" exclude("org.webjars", "jquery"),
  "org.webjars" % "angular-ui-bootstrap" % "0.14.3",
  "org.webjars" % "angular-translate" % "2.7.2",
  "org.webjars" % "angular-translate-loader-static-files" % "2.6.1-1",
  "org.webjars" % "angular-dragdrop" % "1.0.8",
  "org.webjars" % "bootstrap-select" % "1.7.3-1",
  "org.webjars" % "angular-ui-select" % "0.13.1",
  "org.webjars" % "angular-sanitize" % "1.3.11",
  "org.webjars" % "angular-moment" % "0.10.1",
  "org.webjars" % "momentjs" % "2.10.6",
  "org.webjars" % "nvd3-community" % "1.7.0",
  "org.webjars" % "d3js" % "3.5.6",
  "org.webjars" % "angularjs-nvd3-directives" % "0.0.7-1",
  "org.webjars.bower" % "angular-datepicker" % "1.0.12"
)

routesImport ++= Seq(
	"binders.Binders._",
	"play.api.i18n.Lang"
)

javaOptions in Test += "-Dconfig.file=conf/test.conf"

javaOptions in Production += "-Dconfig.file=conf/prod.conf -Dlogger.resource=logback-prod.xml" 

