package me.chadrs.moviescan.clients

import java.time.{LocalDate, LocalDateTime}

import cats.data.OptionT
import cats.effect.IO
import io.circe.{Decoder, Json}

import scala.collection.immutable.Seq
import me.chadrs.moviescan.clients.TmsApi.{MovieShowing, ZipCode}
import org.http4s.{EntityDecoder, Uri}
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.Http4sDsl
import org.http4s.circe._
import io.circe.generic.semiauto.deriveDecoder


object TmsApi {
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
  }
}

class TmsApi(apiKey: String,
             client: Client[IO],
             baseUrl: Uri = Uri.uri("https://data.tmsapi.com/v1.1/"))
    extends Http4sClientDsl[IO]
    with Http4sDsl[IO] {

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
