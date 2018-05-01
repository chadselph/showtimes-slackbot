package me.chadrs.slack

import org.specs2.mutable.Specification
import io.circe.syntax._
import io.circe.literal._

class SlackMessageBuilderTest extends Specification {

  "SlackMessageBuilder" >> {
    "should encode into json" >> {

      import SlackMessageBuilder._
      import SlackMessageBuilder.Implicits._

      val json = response(
        "Create a movie poll!",
        attachment("Choose which movies to include",
                   "movie",
                   select("movie",
                          "movie",
                          option("Black Panther"),
                          option("I, Tonya"))),
        attachment("Choose which theaters to include",
                   "theater",
                   select("theater",
                          "theater",
                          option("AMC Van Ness 14"),
                          option("Alamo Drafthouse")))
      ).asJson
      println(json.spaces2)
      json must_== json"""
                          {}
        """

    }
  }

}
