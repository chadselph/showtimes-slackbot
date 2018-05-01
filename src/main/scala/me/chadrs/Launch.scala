package me.chadrs

import java.util.concurrent.Executors

import cats.effect.IO
import fs2.StreamApp
import org.http4s._
import org.http4s.dsl.Http4sDsl
import io.circe.syntax._
import me.chadrs.moviepoll.ShowtimesPoll
import org.http4s.server.blaze.BlazeBuilder
import me.chadrs.moviescan.clients.AMC
import me.chadrs.slack.{SlackAction, SlackCommand}
import org.http4s.client.blaze.Http1Client
import org.http4s.circe._

import scala.concurrent.ExecutionContext

object Launch extends StreamApp[IO] with Http4sDsl[IO] {

  val amcClient = new AMC(Http1Client[IO]().unsafeRunSync)
  implicit val ec: ExecutionContext =
    ExecutionContext.fromExecutor(Executors.newWorkStealingPool())
  import me.chadrs.slack.SlackMessageBuilder.Implicits._

  val service = HttpService[IO] {
    case GET -> Root / "v1" / "showtimes" =>
      Ok(
        amcClient
          .queryMovies(Seq("amc-van-ness-14", "amc-metreon-16"))
          .map(_.asJson)
      )
    case req @ POST -> Root / "slack" =>
      req.decode[SlackCommand] { data =>
        Ok(ShowtimesPoll.newPoll().asJson)
      }

    case req @ POST -> Root / "slack-menu" =>
      req.decode[SlackAction] { slackAction =>
        Ok(ShowtimesPoll.update(slackAction).asJson)
      }
  }

  def stream(args: List[String],
             requestShutdown: IO[Unit]): fs2.Stream[IO, StreamApp.ExitCode] =
    BlazeBuilder[IO]
      .bindHttp(7688, "0.0.0.0")
      .mountService(service, "/")
      .serve

}
