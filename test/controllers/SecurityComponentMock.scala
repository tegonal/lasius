package controllers

import org.specs2.mock.Mockito

trait SecurityComponentMock extends SecurityComponent with Mockito {
  override val authConfig: AuthConfig = mock[AuthConfig]
}