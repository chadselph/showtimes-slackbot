package me.chadrs.moviescan.clients

import java.time.Instant

import cats.effect.IO
import io.circe.{Encoder, Json}
import io.circe.generic.auto._
import io.circe.syntax._
import me.chadrs.data.Showtime
import org.http4s.Uri
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.Http4sDsl
import org.http4s.circe._

case class GraphQLQuery(query: String,
                        variables: Map[String, String] = Map.empty)




class AMC(client: Client[IO],
          baseUrl: Uri = Uri.uri("https://graph.amctheatres.com/"))
    extends Http4sClientDsl[IO]
    with Http4sDsl[IO] {

  def queryMovies(theaters: Seq[String]): IO[List[Showtime]] = {
    val query = GraphQLQuery(makeGraphQl(theaters))
    println(query.asJson.spaces2)
    client.fetch(POST.apply(baseUrl, query.asJson)) { resp =>
      resp.as[Json].map(decodeGraphJsonToShowTimes)
    }
  }

  def decodeGraphJsonToShowTimes(j: Json): List[Showtime] = {

    implicit class JsonWithStringFromObj(val json: Json) {
      def stringFromObject(key: String): Option[String] =
        json.asObject.flatMap(_(key)).flatMap(_.asString)
      def intFromObject(key: String): Option[Int] =
        json.asObject.flatMap(_(key)).flatMap(_.asNumber).flatMap(_.toInt)
    }

    j.findAllByKey("_movie").flatMap { movieJson =>
      val movieName = movieJson.stringFromObject("name")
      val mpaa = movieJson.stringFromObject("mpaaRating")
      val runtime = movieJson.intFromObject("runTime")
      val rtRating = movieJson.asObject
        .flatMap(_("ratings"))
        .flatMap(_.asObject)
        .flatMap(_("rottenTomatoes"))
        .flatMap(_.asObject)
        .flatMap(_("criticsScore"))
        .flatMap(_.asNumber)
        .flatMap(_.toInt)
      movieJson.findAllByKey("_showtime").flatMap { showtimeJson =>
        val when = showtimeJson.stringFromObject("when")
        val start = when.map(Instant.parse)
        showtimeJson.findAllByKey("_theatre").flatMap { theatreJson =>
          val theatreName = theatreJson.stringFromObject("name")

          (for {
            mn <- movieName
            s <- start
            tn <- theatreName
            rt <- runtime
          } yield Showtime(mn, s, tn, Set(), mpaa, rt, rtRating)).toList
        }
      }
    }
  }

  def makeGraphQl(theaters: Seq[String]): String =

    s"""
       |{
       |  viewer {
      ${
        val theatreSections = for ((th, i) <- theaters.zipWithIndex) yield {
          s"""theatre_$i: theatre(slug: ${Json.fromString(th).noSpaces}) {
              movies {
               ...getShowtimes
             }
          }"""
        }
      theatreSections.mkString("\n")
    }
       |  }
       |}
       |
       |fragment getShowtimes on TheatreMovieConnection {
       |  edges {
       |    _movie: node {
       |      name
       |      runTime
       |      mpaaRating
       |      ratings {
       |        rottenTomatoes {
       |          criticsScore
       |        }
       |      }
       |      formats {
       |        items {
       |          groups {
       |            edges {
       |              node {
       |                showtimes(first: 10) {
       |                  edges {
       |                    _showtime: node {
       |                      _theatre: theatre {
       |                        name
       |                      }
       |                      when: showDateTimeUtc
       |                      isAlmostSoldOut
       |                    }
       |                  }
       |                }
       |              }
       |            }
       |          }
       |        }
       |      }
       |    }
       |  }
       |}
       |
     """.stripMargin

}
