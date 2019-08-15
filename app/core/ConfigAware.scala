package core

import com.typesafe.config.ConfigFactory

trait ConfigAware {
  lazy val config = ConfigFactory.load()
}
