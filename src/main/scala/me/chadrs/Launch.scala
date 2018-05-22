package me.chadrs

import java.nio.file.Paths
import java.time.LocalDate
import java.util.concurrent.Executors

import cats.effect.IO
import fs2.StreamApp
import org.http4s._
import org.http4s.dsl.Http4sDsl
import io.circe.syntax._
import me.chadrs.data.{DailyCachedShowtimes, Showtime, ShowtimeDatabase}
import me.chadrs.moviepoll.ShowtimesPoll
import me.chadrs.moviescan.clients.{TmsApiClient, TmsMovieSearch, TmsShowtimeDatabase}
import org.http4s.server.blaze.BlazeBuilder
import me.chadrs.slack.{SlackAction, SlackCommand}
import org.http4s.client.blaze.Http1Client
import org.http4s.circe._
import io.github.howardjohn.lambda.http4s.Http4sLambdaHandler
import me.chadrs.cachelayers.{CirceFileCache, CirceS3Cache, InMemoryCache}
import software.amazon.awssdk.services.s3.S3AsyncClient

import scala.concurrent.ExecutionContext

object Launch extends StreamApp[IO] with Http4sDsl[IO] {


  val tmsClient: TmsMovieSearch[IO] = new TmsApiClient(System.getenv("TMS_API_KEY"), Http1Client[IO]().unsafeRunSync)

  implicit val ec: ExecutionContext =
    ExecutionContext.fromExecutor(Executors.newWorkStealingPool())
  import me.chadrs.slack.SlackMessageBuilder.Implicits._

  def service(db: ShowtimeDatabase): HttpService[IO] = HttpService[IO] {
    case GET -> Root / "v1" / "showtimes" / IntVar(zip) =>
      loadData(db, zip.toString, LocalDate.now()) { showtimes =>
        Ok(
          showtimes.asJson
        )
      }
    case req @ POST -> Root / "slack" =>
      req.decode[SlackCommand] { data =>
        loadData(db, "94114", LocalDate.now()) { showtimes =>
          Ok(ShowtimesPoll.newPoll(showtimes).asJson)
        }
      }

    case req @ POST -> Root / "slack-menu" =>
      req.decode[SlackAction] { slackAction =>
        loadData(db, "94114", LocalDate.now()) { showtimes =>
          Ok(ShowtimesPoll.update(showtimes, slackAction).asJson)
        }
      }
  }

  private def loadData(db: ShowtimeDatabase, zip: String, time: LocalDate)(cont: Seq[Showtime] => IO[Response[IO]]) = {
    db.getShowTimes(zip, time).flatMap(cont)
  }
  def stream(args: List[String],
             requestShutdown: IO[Unit]): fs2.Stream[IO, StreamApp.ExitCode] =
    BlazeBuilder[IO]
      .bindHttp(7688, "0.0.0.0")
      .mountService(service(
        new DailyCachedShowtimes(new InMemoryCache[IO, (String, LocalDate), Seq[Showtime]](
          new CirceFileCache[IO, (String, LocalDate), Seq[Showtime]](
            { case (zip, localdate) => Paths.get("showtimes-cache", localdate.toString, zip) },
            { case (zip, localdate) => new TmsShowtimeDatabase(tmsClient).getShowTimes(zip, localdate) })
        ))
      ), "/")
      .serve

  class EntryPoint extends Http4sLambdaHandler(service(
    new DailyCachedShowtimes(new InMemoryCache[IO, (String, LocalDate), Seq[Showtime]](
      new CirceS3Cache[(String, LocalDate), Seq[Showtime]](
        S3AsyncClient.create(),
        "movie-showtimes",
        { case (zip, localdate) => s"$localdate/$zip" },
        { case (zip, localdate) => new TmsShowtimeDatabase(tmsClient).getShowTimes(zip, localdate) })
    ))
  ))


}
