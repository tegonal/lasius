import play.sbt.routes.RoutesKeys
import org.scalafmt.sbt.ScalafmtPlugin.autoImport.*
import com.typesafe.sbt.packager.docker.{DockerPermissionStrategy, Cmd, ExecCmd}

name := """lasius"""

Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val root = (project in file("."))
  .enablePlugins(
    PlayScala,
    BuildInfoPlugin,
    SwaggerPlugin,
    AutomateHeaderPlugin,
    JavaAppPackaging,  // Enables basic packaging
    DockerPlugin,      // Enables Docker image generation
    AshScriptPlugin    // Generates ash-compatible scripts for Alpine
  )
  .settings(
    RoutesKeys.routesImport += "binders.Binders._",
    swaggerV3  := true,
    maintainer := "Tegonal Genossenschaft, https://tegonal.com"
  )

swaggerDomainNameSpaces := Seq("models", "controllers")
swaggerPrettyJson       := true
swaggerOutputTransformers += "core.swagger.SwaggerRenameModelClassesTransformer"

scalaVersion := "2.13.18"

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

val pekkoVersion             = "1.4.0"
val reactiveMongoPlayVersion = "1.1.0-play30.RC20"
// Play framework 3.x is still bound to older guice version
val guiceVersion      = "6.0.0"
val pureConfigVersion = "0.17.10"
val jacksonVersion    = "2.21.1"

libraryDependencies ++= Seq(
  "org.reactivemongo" %% "play2-reactivemongo" % reactiveMongoPlayVersion,
  "com.github.scullxbones"      %% "pekko-persistence-mongodb" % "1.4.0",
  "com.tegonal"                 %% "play-json-typedid"         % "2.0.0",
  "org.julienrf"                %% "play-json-derived-codecs"  % "11.0.0",
  "org.playframework"           %% "play-json-joda"            % "3.0.6",
  "com.google.inject"            % "guice"                     % guiceVersion,
  "com.google.inject.extensions" % "guice-assistedinject"      % guiceVersion,
  // support more than 22 fields in case classes
  "org.apache.pekko" %% "pekko-persistence"           % pekkoVersion,
  "org.apache.pekko" %% "pekko-actor"                 % pekkoVersion,
  "org.apache.pekko" %% "pekko-actor-typed"           % pekkoVersion,
  "org.apache.pekko" %% "pekko-serialization-jackson" % pekkoVersion,
  "org.apache.pekko" %% "pekko-persistence-query"     % pekkoVersion,
  "org.apache.pekko" %% "pekko-slf4j"                 % pekkoVersion,
  "org.apache.pekko" %% "pekko-testkit"               % pekkoVersion % "test",
  "org.apache.pekko" %% "pekko-persistence-testkit"   % pekkoVersion % "test",
  // reactivemongo based connector for persistent akka
  "org.mindrot"         % "jbcrypt"                   % "0.4",
  "de.flapdoodle.embed" % "de.flapdoodle.embed.mongo" % "4.24.0" % "test",
  "io.github.alstanchev" % "pekko-persistence-inmemory_2.13" % "1.3.0" % "test",
  "io.kontainers"       %% "purecsv"                         % "1.3.10",
  "com.chuusai"         %% "shapeless"                       % "2.3.13",
  "net.openhft"          % "zero-allocation-hashing"         % "0.27ea1",
  "com.github.pureconfig" %% "pureconfig" % pureConfigVersion,
  // depend on this plugin to be able to provide custom OutputTransformer
  "io.github.play-swagger" %% "play-swagger" % "3.1.7",
  "com.github.fdimuccio"   %% "play2-sockjs" % "0.10.0",

  // basic jwt token and jwks support
  // "com.github.jwt-scala" %% "jwt-play" % "10.0.1",
  "com.auth0" % "java-jwt" % "4.5.1",
  "com.auth0" % "jwks-rsa" % "0.23.0",

  // oauth2 provider dependencies
  // oauth2 provider dependencies to be able to provide a simple oauth server packed with lasius
  "com.nulab-inc" %% "scala-oauth2-core"     % "1.6.0",
  "com.nulab-inc" %% "play2-oauth2-provider" % "2.0.0",
  ehcache,
  ws,
  specs2 % Test,
  guice,
  "org.webjars" % "swagger-ui" % "5.32.1"
)

dependencyOverrides ++= Seq(
  "com.fasterxml.jackson.module" % "jackson-module-scala_2.13" % jacksonVersion,
  "com.fasterxml.jackson.core"   % "jackson-databind"          % jacksonVersion,
  "com.fasterxml.jackson.core"   % "jackson-core"              % jacksonVersion,
  // jackson-annotations dropped patch versions from 2.20+; only publishes minor (e.g. "2.21")
  "com.fasterxml.jackson.core"   % "jackson-annotations"       % "2.21",
)

Test / javaOptions += "-Dconfig.file=conf/test.conf"

Test / console / fork := true

Production / javaOptions.withRank(
  KeyRanks.Invisible) += "-Dconfig.file=conf/prod.conf -Dlogger.resource=logback-prod.xml"

scalafmtOnCompile := true

// ============================================================================
// Compiler Warning Suppressions
// ============================================================================

// Suppress jsObjectWrites deprecation warnings from ReactiveMongo
// Context:
//   - Using play2-reactivemongo 1.1.0-play30.RC17 (no stable release available)
//   - ReactiveMongo's jsObjectWrites is deprecated since 0.20.6 with message:
//     "Will be removed when provided by Play-JSON itself"
//   - This is an internal compatibility layer in the library, not our code
//   - Upgrading won't help - waiting for Play-JSON to provide native support
// Adverse effects: NONE
//   - Only suppresses warnings in library code (reactivemongo package)
//   - Does NOT suppress deprecation warnings in our own code
//   - Does NOT hide actual problems in our codebase
//   - Can be removed when Play-JSON provides native support
// Alternative: Wait for stable play2-reactivemongo release (not yet available)
scalacOptions ++= Seq(
  "-Wconf:cat=deprecation&origin=reactivemongo\\..*&msg=jsObjectWrites:s"
)

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

// ============================================================================
// Docker Configuration (sbt-native-packager)
// ============================================================================

// Base image - Eclipse Temurin 25 JRE on Alpine for small size
dockerBaseImage := "eclipse-temurin:25-jre-alpine"

// Exposed ports
dockerExposedPorts := Seq(9000)

// Docker repository
dockerRepository := Some("tegonal")

// Override package name for Docker (CI/CD expects "lasius-backend")
Docker / packageName := "lasius-backend"

// Sanitize version for Docker tags (Docker doesn't allow '+' characters)
// Preserve 'v' prefix to match frontend tagging convention
import com.typesafe.sbt.packager.docker.DockerAlias
dockerAlias := {
  val rawVersion = version.value.replace('+', '-')
  // sbt-dynver strips 'v' prefix, so add it back if not present
  val sanitizedVersion = if (rawVersion.startsWith("v")) rawVersion else s"v$rawVersion"
  DockerAlias(
    registryHost = dockerRepository.value,
    username = None,
    name = (Docker / packageName).value,
    tag = Some(sanitizedVersion)
  )
}

// Only tag as 'latest' for stable semver releases (e.g., v1.2.3 or 1.2.3)
// Any other tag pattern is considered beta/experimental
dockerAliases ++= {
  val ver = version.value
  // Match semantic versioning: optional 'v' prefix + X.Y.Z (where X, Y, Z are numbers)
  val semverPattern = """^v?\d+\.\d+\.\d+$""".r
  val isStableRelease = semverPattern.pattern.matcher(ver).matches()

  if (isStableRelease) {
    Seq(dockerAlias.value.withTag(Some("latest")))
  } else {
    Seq.empty
  }
}

// Use multi-stage build for better security and smaller images
dockerPermissionStrategy := DockerPermissionStrategy.MultiStage

// Run as non-root user (security best practice)
// Default user is "demiourgos728" with UID 1001 (provided by sbt-native-packager)

// Note: No logs directory needed - using STDOUT logging (Docker best practice)

// Docker labels (including git commit info for traceability)
dockerLabels := {
  val gitCommit = sys.env.getOrElse("COMMIT_SHORT_SHA",
    scala.sys.process.Process("git rev-parse --short=8 HEAD").!!.trim)

  Map(
    "maintainer" -> "Tegonal Genossenschaft <https://tegonal.com>",
    "org.opencontainers.image.title" -> "Lasius Backend",
    "org.opencontainers.image.description" -> "Open source time tracker for teams",
    "org.opencontainers.image.vendor" -> "Tegonal Genossenschaft",
    "org.opencontainers.image.licenses" -> "AGPL-3.0",
    "org.opencontainers.image.source" -> "https://github.com/tegonal/lasius",
    "org.opencontainers.image.revision" -> gitCommit,
    "git-commit" -> gitCommit  // Legacy label for backwards compatibility
  )
}
