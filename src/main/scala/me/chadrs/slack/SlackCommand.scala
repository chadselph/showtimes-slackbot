package me.chadrs.slack

import cats.effect.Sync
import org.http4s.{Charset, DecodeResult, EntityDecoder, InvalidMessageBodyFailure, UrlForm}

case class SlackCommand(channelId: String,
                        channelName: String,
                        command: String,
                        responseUrl: String,
                        teamDomain: String,
                        text: String)

object SlackCommand {

  implicit def entityDecoder[F[_]](implicit F: Sync[F],
                                   defaultCharset: Charset =
                                     org.http4s.DefaultCharset)
    : EntityDecoder[F, SlackCommand] =
    UrlForm.entityDecoder(F, defaultCharset).flatMapR { form =>
      def getOrMissing(field: String): Either[String, String] =
        form.getFirst(field).toRight(s"Missing $field")
      val parsed = for {
        channelId <- getOrMissing("channel_id")
        channelName <- getOrMissing("channel_name")
        command <- getOrMissing("command")
        responseUrl <- getOrMissing("response_url")
        teamDomain <- getOrMissing("team_domain")
        text <- getOrMissing("text")
      } yield {
        SlackCommand(channelId,
                     channelName,
                     command,
                     responseUrl,
                     teamDomain,
                     text)
      }
      parsed.fold(s => DecodeResult.failure(InvalidMessageBodyFailure(s)),
                  DecodeResult.success(_))
    }
}
