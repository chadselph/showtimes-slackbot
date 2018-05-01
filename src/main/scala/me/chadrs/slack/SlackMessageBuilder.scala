package me.chadrs.slack

import io.circe.{Decoder, DecodingFailure, Encoder, Json}
import io.circe.generic.extras.semiauto._
import io.circe.generic.extras.Configuration

object SlackMessageBuilder {

  sealed trait SlackEnum {
    def asString: String = toString.toLowerCase
  }

  sealed trait AttachmentType extends SlackEnum
  object AttachmentType {
    case object Default extends AttachmentType
  }
  sealed trait Style extends SlackEnum
  object Style {
    case object Default extends Style
    case object Primary extends Style
    case object Danger extends Style
  }
  sealed trait Type extends SlackEnum
  object Type {
    case object Select extends Type
    case object Button extends Type
  }

  sealed trait ResponseType extends SlackEnum
  object ResponseType {
    case object InChannel extends ResponseType {
      override def asString: String = "in_channel"
    }
    case object Ephemeral extends ResponseType
  }

  case class SelectOption(text: String,
                          value: String,
                          description: Option[String])

  case class Action(name: String,
                    text: String,
                    `type`: Type,
                    value: Option[String],
                    selectedOptions: Option[Seq[SelectOption]],
                    style: Option[Style],
                    options: Option[Seq[SelectOption]])
  case class Attachment(text: String,
                        fallback: String,
                        callbackId: String,
                        color: Option[String],
                        actions: Seq[Action],
                        attachmentType: Option[AttachmentType])
  case class SlackMessage(text: String,
                          responseType: Option[ResponseType],
                          replaceOriginal: Option[Boolean],
                          deleteOriginal: Option[Boolean],
                          attachments: Seq[Attachment])

  def response(text: String, attachments: Attachment*): SlackMessage = {
    SlackMessage(text, None, None, None, attachments)
  }

  def respondAndReplace(text: String, attachments: Attachment*): SlackMessage = {
    SlackMessage(text, None, Some(true), None, attachments)
  }

  def attachment(text: String,
                 callbackId: String,
                 actions: Action*): Attachment = {
    Attachment(text,
               s"Can't load: $text",
               callbackId,
               None,
               actions,
               None)
  }

  def select(name: String, text: String, options: SelectOption*): Action =
    Action(name, text, Type.Select, None, None, None, Some(options))

  def option(text: String,
             value: Option[String] = None,
             description: Option[String] = None) =
    SelectOption(text, value.getOrElse(text), description)

  def button(name: String, text: String, style: Style = Style.Default): Action =
    Action(name, text, Type.Button, None, None, Some(style), None)

  object Implicits {
    /* circe codecs */
    implicit val config: Configuration =
      Configuration.default.withSnakeCaseMemberNames
    private def encodeSlackEnum[T <: SlackEnum]: Encoder[T] =
      (t: T) => Json.fromString(t.asString)

    implicit val typeEncoder: Encoder[Type] = encodeSlackEnum[Type]
    implicit val styleEncoder: Encoder[Style] = encodeSlackEnum[Style]
    implicit val attachmentTypeEncoder: Encoder[AttachmentType] =
      encodeSlackEnum[AttachmentType]
    implicit val attachmentRespType: Encoder[ResponseType] =
      encodeSlackEnum[ResponseType]
    implicit val selectOptionEncoder: Encoder[SelectOption] = deriveEncoder
    implicit val actionEncoder: Encoder[Action] = deriveEncoder
    implicit val attachmentEncoder: Encoder[Attachment] = deriveEncoder
    implicit val slackMessageEncoder: Encoder[SlackMessage] = deriveEncoder
    // implicit val slackMessageDecoder: Decoder[SlackMessage] = deriveDecoder
  }

}
