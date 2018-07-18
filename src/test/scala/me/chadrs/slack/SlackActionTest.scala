package me.chadrs.slack

import org.specs2.mutable.Specification
import SlackAction._
import io.circe.Decoder
import io.circe.parser.decode

class SlackActionTest extends Specification {

  implicit val decoder: Decoder[SlackAction] = slackActionDecoder

  "SlackAction json decode" >> {
    "should decode this example" >> {

      val example =
        """{"type":"interactive_message","actions":[{"name":"interested",
          |"type":"button","value":"yes"}],"callback_id":"??","team":
          |{"id":"T02TF28E2","domain":"spurint"},"channel":{"id":"GBTCK4F9V",
          |"name":"privategroup"},"user":{"id":"U02TL8NLU","name":"chad"},
          |"action_ts":"1531877809.490927","message_ts":"1531877804.000066"
          |,"attachment_id":"1","token":"sabDXcJaG6oItRVKFA4VtEdT",
          |"is_app_unfurl":false,"response_url":"https:\/\/hooks.slack.com\/actions\/T02TF28E2\/400412846610\/L5zrFFi2EKSbEhGuZSarEwU6",
          |"trigger_id":"400550411141.2933076478.475ea53ef53dc9c6393a692384509c0b"}""".stripMargin

      decode[SlackAction](example) must beRight(
        SlackAction(
          Seq(
            Action("interested",
                   Some("yes"),
                   SlackMessageBuilder.Type.Button,
                   None)),
          "??",
          Team("T02TF28E2", "spurint"),
          Channel("GBTCK4F9V", "privategroup"),
          User("U02TL8NLU", "chad"),
          "1531877809.490927",
          "1531877804.000066",
          "1",
          "sabDXcJaG6oItRVKFA4VtEdT",
          None,
          "https://hooks.slack.com/actions/T02TF28E2/400412846610/L5zrFFi2EKSbEhGuZSarEwU6"
        ))
    }

    "and this other example" >> {
      val otherExample = """{"type":"interactive_message","actions":[{"name":"interested","type":"button","value":"no"}],"callback_id":"??","team":{"id":"T02TF28E2","domain":"spurint"},"channel":{"id":"GBTCK4F9V","name":"privategroup"},"user":{"id":"U02TL8NLU","name":"chad"},"action_ts":"1531882853.517483","message_ts":"1531882849.000161","attachment_id":"1","token":"sabDXcJaG6oItRVKFA4VtEdT","is_app_unfurl":false,"original_message":{"text":"chad has suggested a movie time!\nThe First Purge\nTuesday 10:45PM\nAMC Van Ness 14.","bot_id":"B9AJ7JYAE","attachments":[{"callback_id":"??","fallback":"Can't load: Can you make it?","text":"Can you make it?","id":1,"actions":[{"id":"1","name":"interested","text":"yes","type":"button","value":"yes","style":"primary"},{"id":"2","name":"interested","text":"no","type":"button","value":"no","style":"danger"}]},{"callback_id":"1","fallback":"Can't load: chad says yes","text":"chad says yes","id":2}],"type":"message","subtype":"bot_message","ts":"1531882849.000161"},"response_url":"https:\/\/hooks.slack.com\/actions\/T02TF28E2\/399879908353\/FYVTzF265TQJlKUL0Fyj0iQD","trigger_id":"401472318551.2933076478.1e80111ce96d9175b08d7fc22916f922"}"""
      decode[SlackAction](otherExample) must beRight
    }
  }

}
