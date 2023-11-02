/*
 *
 * Lasius - Open source time tracker for teams
 * Copyright (c) Tegonal Genossenschaft (https://tegonal.com)
 *
 * This file is part of Lasius.
 *
 * Lasius is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * Lasius is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Lasius. If not, see <https://www.gnu.org/licenses/>.
 */

package modules

import com.google.inject.{AbstractModule, Provides}
import com.typesafe.config.{Config => TypesafeConfig}
import org.pac4j.config.client.PropertiesConfigFactory
import org.pac4j.core.config.Config
import org.pac4j.core.context.session.{SessionStore, SessionStoreFactory}
import org.pac4j.core.matching.matcher.PathMatcher
import org.pac4j.http.client.direct.{DirectBearerAuthClient, HeaderClient}
import org.pac4j.jwt.config.encryption.SecretEncryptionConfiguration
import org.pac4j.jwt.config.signature.SecretSignatureConfiguration
import org.pac4j.jwt.credentials.authenticator.JwtAuthenticator
import org.pac4j.play.http.PlayHttpActionAdapter
import org.pac4j.play.scala.{DefaultSecurityComponents, SecurityComponents}
import org.pac4j.play.store.{PlayCookieSessionStore, ShiroAesDataEncrypter}
import org.pac4j.play.{CallbackController, LogoutController}
import play.api.{Configuration, Environment}

import java.nio.charset.StandardCharsets
import scala.jdk.CollectionConverters.{
  CollectionHasAsScala,
  ListHasAsScala,
  MapHasAsJava
}

/** Guice DI module to be included in application.conf
  */
class SecurityModule(environment: Environment, configuration: Configuration)
    extends AbstractModule {

  val baseUrl = configuration.get[String]("baseUrl")

  override def configure(): Unit = {

    val sKey =
      configuration.get[String]("play.http.secret.key").substring(0, 16)
    val dataEncrypter = new ShiroAesDataEncrypter(
      sKey.getBytes(StandardCharsets.UTF_8))
    val playSessionStore = new PlayCookieSessionStore(dataEncrypter)
    bind(classOf[SessionStore]).toInstance(playSessionStore)
    bind(classOf[SecurityComponents]).to(classOf[DefaultSecurityComponents])

    // callback
    val callbackController = new CallbackController()
    callbackController.setDefaultUrl("/")
    bind(classOf[CallbackController]).toInstance(callbackController)

    // logout
    val logoutController = new LogoutController()
    logoutController.setDefaultUrl("/")
    bind(classOf[LogoutController]).toInstance(logoutController)
  }

  @Provides
  def jwtAccessTokenClient(): HeaderClient = {
    val signingKey = configuration.get[String]("lasius.auth.jwt.signing_key")
    val encryptionKey =
      configuration.get[String]("lasius.auth.jwt.encryption_key")

    val jwtAuthenticator = new JwtAuthenticator()
    jwtAuthenticator.addSignatureConfiguration(
      new SecretSignatureConfiguration(signingKey))

    jwtAuthenticator.addEncryptionConfiguration(
      new SecretEncryptionConfiguration(encryptionKey))

    new HeaderClient("X-LASIUS-AUTH", jwtAuthenticator)
  }

  @Provides
  def provideConfig(jwtAccessTokenClient: HeaderClient,
                    sessionStore: SessionStore): Config = {
    val callbackUrl = configuration.get[String]("lasius.auth.callback_url")

    val cfg: TypesafeConfig =
      configuration.underlying.getConfig("lasius.auth.clients")
    val clientsConfiguration =
      cfg
        .entrySet()
        .asScala
        .map(entry => entry.getKey -> entry.getValue.unwrapped().toString)
        .toMap
    val configFactory =
      new PropertiesConfigFactory(callbackUrl, clientsConfiguration.asJava)

    val config = configFactory.build()

    // add by default jwt access token client
    config.getClients.addClient(jwtAccessTokenClient)

    // by default, allow authentication with all kind of clients
    config.getClients.setDefaultSecurityClients(
      config.getClients.getClients.asScala.map(_.getName).mkString(","))

    // config.addAuthorizer("admin", new RequireAnyRoleAuthorizer("ROLE_ADMIN"))
    // config.addAuthorizer("custom", new CustomAuthorizer)
    config.addMatcher("excludedPath",
                      new PathMatcher()
                        .excludeBranch("/backend/oauth2")
                        .excludeBranch("/backend/auth/callback"))

    config.setSessionStoreFactory(new SessionStoreFactory {
      override def newSessionStore(parameters: AnyRef*): SessionStore =
        sessionStore
    });
    config.setHttpActionAdapter(new PlayHttpActionAdapter())
    config
  }
}
