package me.chadrs.moviescan.clients

import java.time.{Instant, LocalDate, LocalDateTime}

import cats.effect.IO
import io.circe.{Decoder, Encoder}

import scala.collection.immutable.Seq
import me.chadrs.moviescan.clients.Tms.{MovieShowing, ZipCode}
import org.http4s.{EntityDecoder, Uri}
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.Http4sDsl
import org.http4s.circe._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import me.chadrs.data.{Showtime, ShowtimeDatabase}


object Tms {
  type ZipCode = String
  type OptionalList[T] = Option[Seq[T]]

  case class Rating(body: String, code: String)
  case class Image(uri: Uri)
  case class Theater(id: String, name: String)
  case class Showtime(theatre: Theater, quals: Option[String], ticketURI: Option[Uri], dateTime: LocalDateTime)

  case class MovieShowing(tmsId: String,
                          rootId: String,
                          title: String,
                          releaseYear: Option[Int],
                          releaseDate: Option[String],
                          genres: OptionalList[String],
                          longDescription: Option[String],
                          shortDescription: Option[String],
                          topCast: OptionalList[String],
                          ratings: OptionalList[Rating],
                          preferredImage: Option[Image],
                          showtimes: Seq[Showtime])

  object MovieShowing {
    implicit val decodeLdt: Decoder[LocalDateTime] = Decoder.decodeString.map(LocalDateTime.parse)
    implicit val decoderShowtime: Decoder[Showtime] = deriveDecoder[Showtime]
    implicit val decoderRating: Decoder[Rating] = deriveDecoder[Rating]
    implicit val decoderImage: Decoder[Image] = deriveDecoder[Image]
    implicit val decoderTheater: Decoder[Theater] = deriveDecoder[Theater]
    implicit val decoder: Decoder[MovieShowing] = deriveDecoder[MovieShowing]
    implicit val movieShowingDecoder: EntityDecoder[IO, Seq[MovieShowing]] = jsonOf[IO, Seq[MovieShowing]]

    implicit val encodeLdt: Encoder[LocalDateTime] = Encoder.encodeString.contramap(_.toString)
    implicit val encoderShowtime: Encoder[Showtime] = deriveEncoder[Showtime]
    implicit val encoderRating: Encoder[Rating] = deriveEncoder[Rating]
    implicit val encoderImage: Encoder[Image] = deriveEncoder[Image]
    implicit val encoderTheater: Encoder[Theater] = deriveEncoder[Theater]
    implicit val encoder: Encoder[MovieShowing] = deriveEncoder[MovieShowing]
  }
}

trait TmsMovieSearch[F[_]] {
  def showings(startDate: LocalDate, zip: ZipCode): F[Seq[MovieShowing]]
}

class TmsApiClient(apiKey: String,
             client: Client[IO],
             baseUrl: Uri = Uri.uri("https://data.tmsapi.com/v1.1/"),
                  )
  extends Http4sClientDsl[IO]
    with Http4sDsl[IO]
    with TmsMovieSearch[IO] {

  def showings(startDate: LocalDate, zip: ZipCode): IO[Seq[MovieShowing]] = {
    val showingsPath = (baseUrl / "movies" / "showings")
      .withQueryParam("zip", zip)
      .withQueryParam("api_key", apiKey)
      .withQueryParam("startDate", startDate.toString)
    client.get(showingsPath) { resp =>
      resp.as[Seq[MovieShowing]]
    }
  }

}

class SFShowtimeDatabase(api: TmsMovieSearch[IO]) extends ShowtimeDatabase {

  private val Zip = "94114"
  private val Timezone = java.time.ZoneId.of("America/Los_Angeles")

  override def getShowTimes(zip: String = Zip, date: LocalDate = LocalDate.now()): IO[scala.Seq[Showtime]] = {
    api.showings(date, zip)
      .map { showings =>
        showings.flatMap { movie =>
          movie.showtimes.map { st =>
            val zoneOffset = Timezone.getRules.getOffset(Instant.now())
            Showtime(movie.title, st.dateTime.toInstant(zoneOffset), st.theatre.name, Set(), None, 0, None)
          }
        }
      }
  }
}