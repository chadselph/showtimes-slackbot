package me.chadrs

import java.nio.file.Paths
import java.util.concurrent.Executors

import cats.effect.IO
import fs2.StreamApp
import org.http4s._
import org.http4s.dsl.Http4sDsl
import io.circe.syntax._
import me.chadrs.data.{DailyCachedShowtimes, ShowtimeDatabase}
import me.chadrs.moviepoll.ShowtimesPoll
import me.chadrs.moviescan.clients.{SFShowtimeDatabase, TmsApiClient, TmsMovieSearch}
import org.http4s.server.blaze.BlazeBuilder
import me.chadrs.slack.{SlackAction, SlackCommand}
import org.http4s.client.blaze.Http1Client
import org.http4s.circe._
import io.github.howardjohn.lambda.http4s.Http4sLambdaHandler


import scala.concurrent.ExecutionContext

object Launch extends StreamApp[IO] with Http4sDsl[IO] {


  val tmsClient: TmsMovieSearch[IO] = new TmsApiClient(System.getenv("TMS_API_KEY"), Http1Client[IO]().unsafeRunSync)
  val showtimes: ShowtimeDatabase = new DailyCachedShowtimes(new SFShowtimeDatabase(tmsClient), Some(Paths.get("showtimes-cache")))

  implicit val ec: ExecutionContext =
    ExecutionContext.fromExecutor(Executors.newWorkStealingPool())
  import me.chadrs.slack.SlackMessageBuilder.Implicits._

  val service = HttpService[IO] {
    case GET -> Root / "v1" / "showtimes" =>
      Ok(
        //tmsClient.showings(LocalDate.now(), "94114").map(_.asJson)
        showtimes.getShowTimes().map(_.asJson)
      )
    case req @ POST -> Root / "slack" =>
      req.decode[SlackCommand] { data =>
        Ok(ShowtimesPoll.newPoll(showtimes.getShowTimes().unsafeRunSync()).asJson)
      }

    case req @ POST -> Root / "slack-menu" =>
      req.decode[SlackAction] { slackAction =>
        Ok(ShowtimesPoll.update(showtimes.getShowTimes().unsafeRunSync(), slackAction).asJson)
      }
  }

  def stream(args: List[String],
             requestShutdown: IO[Unit]): fs2.Stream[IO, StreamApp.ExitCode] =
    BlazeBuilder[IO]
      .bindHttp(7688, "0.0.0.0")
      .mountService(service, "/")
      .serve

  class EntryPoint extends Http4sLambdaHandler(service)


}
