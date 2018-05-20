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
       {
         "text" : "Create a movie poll!",
         "response_type" : null,
         "replace_original" : null,
         "delete_original" : null,
         "attachments" : [
           {
             "text" : "Choose which movies to include",
             "fallback" : "Can't load: Choose which movies to include",
             "callback_id" : "movie",
             "color" : null,
             "actions" : [
               {
                 "name" : "movie",
                 "text" : "movie",
                 "type" : "select",
                 "value" : null,
                 "selected_options" : null,
                 "style" : null,
                 "options" : [
                   {
                     "text" : "Black Panther",
                     "value" : "Black Panther",
                     "description" : null
                   },
                   {
                     "text" : "I, Tonya",
                     "value" : "I, Tonya",
                     "description" : null
                   }
                 ]
               }
             ],
             "attachment_type" : null
           },
           {
             "text" : "Choose which theaters to include",
             "fallback" : "Can't load: Choose which theaters to include",
             "callback_id" : "theater",
             "color" : null,
             "actions" : [
               {
                 "name" : "theater",
                 "text" : "theater",
                 "type" : "select",
                 "value" : null,
                 "selected_options" : null,
                 "style" : null,
                 "options" : [
                   {
                     "text" : "AMC Van Ness 14",
                     "value" : "AMC Van Ness 14",
                     "description" : null
                   },
                   {
                     "text" : "Alamo Drafthouse",
                     "value" : "Alamo Drafthouse",
                     "description" : null
                   }
                 ]
               }
             ],
             "attachment_type" : null
           }
         ]
       }
       """

    }
  }

}
