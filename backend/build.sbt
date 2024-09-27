import play.sbt.routes.RoutesKeys
import org.scalafmt.sbt.ScalafmtPlugin.autoImport._

name := """lasius"""

Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val root = (project in file("."))
  .enablePlugins(PlayScala,
                 BuildInfoPlugin,
                 SwaggerPlugin,
                 AutomateHeaderPlugin)
  .settings(
    RoutesKeys.routesImport += "binders.Binders._",
    swaggerV3 := true
  )

swaggerDomainNameSpaces := Seq("models", "controllers")
swaggerPrettyJson       := true
swaggerOutputTransformers += "core.swagger.SwaggerRenameModelClassesTransformer"

scalaVersion := "2.13.12"

buildInfoKeys := Seq[BuildInfoKey](organization,
                                   name,
                                   version,
                                   BuildInfoKey.action("gitVersion") {
                                     dynverGitDescribeOutput
                                   })

buildInfoPackage := "version"

resolvers += "Tegonal releases".at(
  "https://github.com/tegonal/tegonal-mvn/raw/main/releases/")

resolvers += "Sonatype OSS Releases".at(
  "https://oss.sonatype.org/content/repositories/releases")

val akkaVersion              = "2.6.21"
val reactiveMongoVersion     = "1.0.10"
val reactiveMongoPlayVersion = s"$reactiveMongoVersion-play28"
val playVersion              = "2.8.20"

libraryDependencies ++= Seq(
  ("org.reactivemongo" %% "play2-reactivemongo" % reactiveMongoPlayVersion)
    .exclude("org.apache.logging.log4j", "log4j-api"),
  "com.github.scullxbones"      %% "akka-persistence-mongo-rxmongo" % "3.1.2",
  "com.tegonal"                 %% "play-json-typedid"              % "1.0.3",
  "org.julienrf"                %% "play-json-derived-codecs"       % "10.1.0",
  "com.typesafe.play"           %% "play-json-joda"                 % "2.10.1",
  "com.google.inject"            % "guice"                          % "5.1.0",
  "com.google.inject.extensions" % "guice-assistedinject"           % "5.1.0",
  // support more than 22 fields in case classes
  "com.typesafe.akka" %% "akka-persistence"       % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence-query" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j"             % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit"           % akkaVersion % "test",
  // reativemongo based connector for persistent akka
  "org.mindrot"          % "jbcrypt"                   % "0.4",
  "de.flapdoodle.embed"  % "de.flapdoodle.embed.mongo" % "4.9.2"    % "test",
  "com.github.dnvriend" %% "akka-persistence-inmemory" % "2.5.15.2" % "test",
  "io.kontainers"       %% "purecsv"                   % "1.3.10",
  "com.chuusai"         %% "shapeless"                 % "2.3.10",
  "net.openhft"          % "zero-allocation-hashing"   % "0.16",
  // depend on this plugin to be able to provide custom OutputTransformer
  "io.github.play-swagger" %% "play-swagger" % "1.4.4",
  ehcache,
  ws,
  specs2 % Test,
  guice,
  "org.webjars" % "swagger-ui" % "5.9.0"
)

dependencyOverrides ++= Seq(
  "com.fasterxml.jackson.module" % "jackson-module-scala_2.13" % "2.18.0",
)

Test / javaOptions += "-Dconfig.file=conf/test.conf"

Test / console / fork := true

Production / javaOptions.withRank(
  KeyRanks.Invisible) += "-Dconfig.file=conf/prod.conf -Dlogger.resource=logback-prod.xml"

scalafmtOnCompile := true

headerLicense := Some(
  HeaderLicense.Custom(
    """|
       |Lasius - Open source time tracker for teams
       |Copyright (c) Tegonal Genossenschaft (https://tegonal.com)
       |
       |This file is part of Lasius.
       |
       |Lasius is free software: you can redistribute it and/or modify it under the
       |terms of the GNU Affero General Public License as published by the Free
       |Software Foundation, either version 3 of the License, or (at your option)
       |any later version.
       |
       |Lasius is distributed in the hope that it will be useful, but WITHOUT ANY
       |WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
       |FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
       |details.
       |
       |You should have received a copy of the GNU Affero General Public License
       |along with Lasius. If not, see <https://www.gnu.org/licenses/>.
       |""".stripMargin
  ))
