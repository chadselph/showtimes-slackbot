package me.chadrs

import java.nio.file.Paths
import java.time.LocalDate
import java.util.concurrent.Executors

import cats.effect.IO
import fs2.StreamApp
import io.circe.syntax._
import io.github.howardjohn.lambda.http4s.Http4sLambdaHandler
import me.chadrs.TimingLogger.time
import me.chadrs.cachelayers.InMemoryCache
import me.chadrs.data.{DailyCachedShowtimes, Showtime, ShowtimeDatabase}
import me.chadrs.moviepoll.ShowtimesPoll
import me.chadrs.moviescan.clients.{TmsMovieSearch, TmsShowtimeDatabase}
import me.chadrs.slack.{SlackAction, SlackCommand}
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.blaze.BlazeBuilder

import scala.concurrent.ExecutionContext

object Launch extends StreamApp[IO] with Http4sDsl[IO] {

  val tmsClient: TmsMovieSearch[IO] = TmsMovieSearch.fromEnv()

  implicit val ec: ExecutionContext =
    ExecutionContext.fromExecutor(Executors.newWorkStealingPool())
  import me.chadrs.slack.SlackMessageBuilder.Implicits._

  def service(db: ShowtimeDatabase): HttpService[IO] = HttpService[IO] {
    case GET -> Root / "v1" / "showtimes" / IntVar(zip) =>
      loadData(db, zip.toString, PacificTime.today()) { showtimes =>
        Ok(
          showtimes.asJson
        )
      }
    case req @ POST -> Root / "slack" =>
      import me.chadrs.moviepoll.ShowtimeCommands._
      req.decode[SlackCommand] { data =>
        loadData(db, "94114", PacificTime.today()) { showtimes =>
          val resp = data match {
            case MovieShowtimesCmd(movie) =>
              ShowtimesPoll.listShowtimes(showtimes, Seq(movie), Nil, PacificTime.zonedDateTime())
            case AllMoviesCmd(_) =>
              ShowtimesPoll.listMovies(showtimes)
            case AllTheatersCmd(_) =>
              ShowtimesPoll.listTheaters(showtimes)
            case TheaterShowtimesCmd(theater) =>
              ShowtimesPoll.listShowtimes(showtimes, Nil, Seq(theater), PacificTime.zonedDateTime())
            case _ =>
              time("newPoll") {
                ShowtimesPoll.newPoll(showtimes)
              }
          }
          Ok(time("asJson")(resp.asJson))
        }
      }

    case req @ POST -> Root / "slack-menu" =>
      req.decode[SlackAction] { slackAction =>
        loadData(db, "94114", PacificTime.today()) { showtimes =>
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
          Caching.fileBackedCache(Paths.get("showtimes-cache"), new TmsShowtimeDatabase(tmsClient))
        ))
      ), "/")
      .serve

  class EntryPoint extends Http4sLambdaHandler(service(
    new DailyCachedShowtimes(new InMemoryCache[IO, (String, LocalDate), Seq[Showtime]](
      Caching.s3BackedCache(Caching.BucketName, new TmsShowtimeDatabase(tmsClient))
  ))))


}
