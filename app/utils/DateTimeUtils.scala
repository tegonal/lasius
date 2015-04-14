package utils

import org.joda.time.DateTime

object DateTimeUtils {
  implicit class ExtendedDateTime(self: DateTime) {

    def withTimeAtEndOfDay: DateTime = {
      return self.withHourOfDay(23).withMinuteOfHour(59).withSecondOfMinute(59).withMillisOfSecond(999);
    }

  }
}