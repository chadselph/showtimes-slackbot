package me.chadrs.data

import java.time.Instant

import io.circe.{Encoder, Json}

case class Showtime(movieName: String,
                    start: Instant,
                    theaterName: String,
                    attributes: Set[String],
                    mpaaRating: Option[String],
                    runtime: Int,
                    rottenTomatoScore: Option[Int])

object Showtime {
  implicit val instantEncoder: Encoder[Instant] = (a: Instant) => Json.fromString(a.toString)
  implicit val showtimeEncoder: Encoder[Showtime] = io.circe.generic.semiauto.deriveEncoder[Showtime]
}