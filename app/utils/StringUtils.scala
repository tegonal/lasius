package utils

object StringUtils {
  implicit class ExString(self: String) {

    def quote: String = "\"" + self + "\""

  }

  implicit class ExOptionString(self: Option[String]) {

    def quote: String = {
      self.map(_.quote).getOrElse("")
    }

  }

}