package me.chadrs.slack

import cats.effect.Sync
import io.circe.{Decoder, Json}
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.auto._
import me.chadrs.slack.SlackAction.{Action, Channel, Team, User}
import me.chadrs.slack.SlackMessageBuilder.{Attachment, Style, Type}
import org.http4s.{
  Charset,
  DecodeResult,
  EntityDecoder,
  InvalidMessageBodyFailure,
  UrlForm
}

case class SlackAction(actions: Seq[Action],
                       callbackId: String,
                       team: Team,
                       channel: Channel,
                       user: User,
                       actionTs: String,
                       messageTs: String,
                       attachmentId: String,
                       token: String,
                       originalMessage: OriginalMessage,
                       responseUrl: String)

case class OriginalMessage(
    attachments: Seq[Attachment],
    botId: String,
    subtype: String,
    text: String,
    ts: String,
    `type`: String
)

object SlackAction {
  case class User(id: String, name: String)
  case class Team(id: String, domain: String)
  case class Channel(id: String, name: String)
  case class SelectedOption(value: String)
  case class Action(name: String,
                    value: Option[String],
                    `type`: Type,
                    selectedOptions: Option[Seq[SelectedOption]])

  implicit val config: Configuration =
    Configuration.default.withSnakeCaseMemberNames.withSnakeCaseConstructorNames
  implicit def entityDecoder[F[_]](implicit F: Sync[F],
                                   defaultCharset: Charset =
                                     org.http4s.DefaultCharset)
    : EntityDecoder[F, SlackAction] = {

    implicit val typeDecoder: Decoder[Type] = Decoder.decodeString.emap {
      case "select" => Right(Type.Select)
      case "button" => Right(Type.Button)
      case unk      => Left(s"Unknown type: $unk")
    }

    implicit val decodeStyle: Decoder[Style] = Decoder.decodeString.emap {
      case "default" => Right(Style.Default)
      case "primary" => Right(Style.Primary)
      case "danger"  => Right(Style.Danger)
      case unk       => Left(s"Unknown style: $unk")
    }

    def decodeFormParam(param: String,
                        form: UrlForm): DecodeResult[F, String] = {
      form
        .getFirst(param)
        .fold(DecodeResult.failure[F, String](
          InvalidMessageBodyFailure("payload not found")))(payload =>
          DecodeResult.success(payload))
    }
    def decodeJson(s: String): DecodeResult[F, Json] =
      io.circe.jawn
        .parse(s)
        .fold(failure =>
                DecodeResult.failure(
                  InvalidMessageBodyFailure(failure.message, Some(failure))),
              DecodeResult.success(_))

    def decodeJsonObj(j: Json): DecodeResult[F, SlackAction] =
      j.as[SlackAction]
        .fold(failure =>
                DecodeResult.failure(
                  InvalidMessageBodyFailure(s"could not decode json: $failure",
                                            Some(failure))),
              DecodeResult.success(_))

    UrlForm
      .entityDecoder(F, defaultCharset)
      .flatMapR { form: UrlForm =>
        decodeFormParam("payload", form)
      }
      .flatMapR(decodeJson)
      .flatMapR(decodeJsonObj)
  }

  object SelectedOption {
    def unapply(arg: SlackAction): Option[(String, String)] = arg match {
      case SlackAction(
          Seq(
            SlackAction.Action(field, _, _, Some(Seq(SelectedOption(value))))),
          _,
          _,
          _,
          _,
          _,
          _,
          _,
          _,
          _,
          _) =>
        Some((field, value))
      case SlackAction(Seq(SlackAction.Action(field, Some(value), _, _)),
                       _,
                       _,
                       _,
                       _,
                       _,
                       _,
                       _,
                       _,
                       _,
                       _) =>
        Some((field, value))
      case _ => None
    }
  }

}
