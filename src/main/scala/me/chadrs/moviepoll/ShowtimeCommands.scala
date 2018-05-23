package me.chadrs.moviepoll

import me.chadrs.slack.SlackCommand

trait ShowtimeCommand {
  def unapply(arg: SlackCommand): Option[String]
}

object ShowtimeCommands {

  def prefixCmd(prefix: String): ShowtimeCommand = (arg: SlackCommand) => if (arg.text.startsWith(prefix)) {
    Some(arg.text.drop(prefix.length))
  } else None

  val MovieShowtimesCmd: ShowtimeCommand = prefixCmd("for ")
  val AllMoviesCmd: ShowtimeCommand = prefixCmd("movies")
  val AllTheatersCmd: ShowtimeCommand = prefixCmd("theaters")
  val TheaterShowtimesCmd: ShowtimeCommand = prefixCmd("at ")


}
