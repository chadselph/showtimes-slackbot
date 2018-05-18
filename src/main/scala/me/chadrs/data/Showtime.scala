package me.chadrs.data

import java.time.{Instant, LocalDateTime}

import io.circe.{Decoder, Encoder, Json}

case class Showtime(movieName: String,
                    start: Instant,
                    theaterName: String,
                    attributes: Set[String],
                    mpaaRating: Option[String],
                    runtime: Int,
                    rottenTomatoScore: Option[Int])

object Showtime {
  implicit val decodeLdt: Decoder[LocalDateTime] = Decoder.decodeString.map(LocalDateTime.parse)
  implicit val encodeLdt: Encoder[LocalDateTime] = (a: LocalDateTime) => Json.fromString(a.toString)
  implicit val decodeInstant: Decoder[Instant] = Decoder.decodeString.map(Instant.parse)
  implicit val instantEncoder: Encoder[Instant] = (a: Instant) => Json.fromString(a.toString)
  implicit val showtimeEncoder: Encoder[Showtime] = io.circe.generic.semiauto.deriveEncoder[Showtime]
  implicit val showtimeDecoder: Decoder[Showtime] = io.circe.generic.semiauto.deriveDecoder[Showtime]
}