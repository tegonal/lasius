import play.sbt.PlayImport.PlayKeys._

name := """lasius"""

version := "1.0-SNAPSHOT"

lazy val jiraScalaApi = RootProject(uri("git://github.com/toggm/play-scala-jira-api.git"))

lazy val root = (project in file(".")).dependsOn(jiraScalaApi).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

resolvers += "Tegonal releases" at "https://github.com/tegonal/tegonal-mvn/raw/master/releases/"

resolvers += "Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases"

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

resolvers += "dnvriend at bintray" at "http://dl.bintray.com/dnvriend/maven"

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

val akkaVs = "2.4.17"
val reactiveMongoVer = "0.11.14"
val kamonV = "0.6.3"

routesGenerator := StaticRoutesGenerator

libraryDependencies ++= Seq(
  "org.reactivemongo" %% "play2-reactivemongo" % reactiveMongoVer,
  "com.tegonal" %% "play-json-typedid" % "1.0.1",
  "org.julienrf" %% "play-json-variants" % "1.1.0",
  "com.typesafe.akka" %% "akka-persistence" % akkaVs  withSources(),
  "com.typesafe.akka" %% "akka-persistence-query-experimental" % akkaVs  withSources(),  
  "com.typesafe.akka" %% "akka-slf4j" % akkaVs,  
  "com.typesafe.akka" %% "akka-testkit" % akkaVs % "test", 
  "com.github.scullxbones" %% "akka-persistence-mongo-rxmongo" % "1.3.7" withSources(),
  "com.github.scullxbones" %% "akka-persistence-mongo-common" % "1.3.7" withSources(), 
  //reativemongo based connector for persistent akka  
  "org.mindrot" % "jbcrypt" % "0.3m",
  "com.github.athieriot" %% "specs2-embedmongo" % "0.7.0" % "test",
  "de.flapdoodle.embed" % "de.flapdoodle.embed.mongo" % "2.0.0" % "test",
  "com.github.dnvriend" %% "akka-persistence-inmemory" % "2.4.17.1" % "test",
  //Akka monitoring  
  "org.aspectj" % "aspectjweaver" % "1.8.7",
  "io.kamon" %% "kamon-core" % kamonV,  
  "io.kamon" %% "kamon-statsd" % kamonV,
  "io.kamon" %% "kamon-play-25" % kamonV,
  "io.kamon" %% "kamon-system-metrics" % kamonV,
  "io.kamon" %% "kamon-log-reporter"   % kamonV,
  "com.typesafe.play.plugins" %% "play-statsd" % "2.3.0",
  "com.github.melrief" %% "purecsv" % "0.0.9",
  "com.chuusai" %% "shapeless" % "2.3.2",
  cache,
  ws,
  specs2 % Test,
  // WebJars (i.e. client-side) dependencies
  "org.webjars" % "requirejs" % "2.3.2",
  "org.webjars" % "underscorejs" % "1.8.3",
  "org.webjars" % "jquery" % "2.1.4",
  "org.webjars.bower" % "bootstrap" % "3.3.6" exclude("org.webjars", "jquery"),
  "org.webjars" % "angularjs" % "1.4.8" exclude("org.webjars", "jquery"),
  "org.webjars" % "angular-ui-bootstrap" % "0.14.3",
  "org.webjars" % "angular-translate" % "2.13.1",
  "org.webjars" % "angular-translate-loader-static-files" % "2.13.1",
  "org.webjars.bower" % "angular-dragdrop" % "1.0.13",
  "org.webjars.bower" % "bootstrap-select" % "1.12.2",
  "org.webjars" % "angular-ui-select" % "0.19.6",
  "org.webjars.bower" % "angular-sanitize" % "1.4.8",
  "org.webjars.bower" % "angular-moment" % "1.0.0-beta.5",
  "org.webjars.bower" % "momentjs" % "2.17.1",
  "org.webjars.bower" % "moment-timezone" % "0.5.11",
  "org.webjars" % "nvd3-community" % "1.7.0",
  "org.webjars" % "d3js" % "3.5.10",
  "org.webjars" % "angular-nvd3" % "0.1.1",
  "org.webjars.bower" % "angular-datepicker" % "1.0.20",
  "org.webjars" % "font-awesome" % "4.7.0"
)

routesImport ++= Seq(
	"binders.Binders._",
	"play.api.i18n.Lang"
)

javaOptions in Test += "-Dconfig.file=conf/test.conf"

javaOptions in Production += "-Dconfig.file=conf/prod.conf -Dlogger.resource=logback-prod.xml" 

