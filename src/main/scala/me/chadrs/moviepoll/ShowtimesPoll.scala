package me.chadrs.moviepoll

import java.time.format.{DateTimeFormatter, TextStyle}
import java.time.{Instant, LocalDate, LocalTime, ZonedDateTime}
import java.util.Locale

import me.chadrs.PacificTime
import me.chadrs.data.Showtime
import me.chadrs.slack.SlackAction
import me.chadrs.slack.SlackMessageBuilder.{SlackMessage, _}
import io.circe.syntax._

object ShowtimesPoll {

  def newPoll(showtimes: Seq[Showtime]): SlackMessage = {
    val movies = showtimes
      .map(_.movieName)
      .distinct
      .map(option(_))
    val theaters = showtimes
      .map(_.theaterName)
      .distinct
      .map(option(_))
    val hours =
      select("time", "time", 1.to(10).map(h => option(s"${h}pm")): _*)
    val days =
      List("Today", "Tomorrow", "Day after tomorrow").zipWithIndex.map {
        case (s, index) =>
          button("create",
                 s,
                 Some(PacificTime.today().plusDays(index).toString),
                 Style.Primary)
      }
    response(
      "Create a movie poll!",
      attachment("Choose which movies to include (up to 4)",
                 "movie",
                 select("movie", "movie", movies: _*)),
      attachment("Choose which theaters to include (up to 4)",
                 "theater",
                 select("theater", "theater", theaters: _*)),
      attachment("Anytime after", "time", hours),
      attachment("Create", "create", days: _*)
    ).copy(responseType = Some(ResponseType.InChannel))
  }

  def update(showtimes: Seq[Showtime], input: SlackAction): SlackMessage = {
    input match {
      case sa @ SlackAction.SelectedOption("movie", movie) =>
        respondAndReplace(
          s"Added $movie",
          updateMultiselect(sa.originalAttachments, "movie", movie): _*
        )
      case sa @ SlackAction.SelectedOption("theater", theater) =>
        respondAndReplace(s"Added $theater",
                          updateMultiselect(sa.originalAttachments,
                                            "theater",
                                            theater): _*)

      case sa @ SlackAction.SelectedOption("time", time) =>
        val updatedAttachments = sa.originalAttachments.map {
          case att if att.callbackId.contains("time") =>
            att.copy(
              actions = Some(att.actions.getOrElse(Nil).map(
                _.copy(selectedOptions = Some(Seq(
                  SelectOption(time, time, None)
                ))))))
          case other => other
        }
        respondAndReplace(s"set time to after $time", updatedAttachments: _*)

      case sa @ SlackAction.SelectedOption("create", dayString) =>
        val form = readMultiselectForm(sa.originalAttachments)
        val simpleHourFormat = DateTimeFormatter.ofPattern("ha")
        val hour = form.get("time").flatMap(_.headOption).getOrElse("12am")
        val day = LocalDate.parse(dayString)
        val after = day
          .atTime(LocalTime.parse(hour.toUpperCase, simpleHourFormat))
          .atZone(PacificTime.tz)
        val title =
          s"Showtimes for ${day.getDayOfWeek.getDisplayName(TextStyle.FULL, Locale.US)}"
        listShowtimes(showtimes,
                      form.getOrElse("movie", Nil),
                      form.getOrElse("theater", Nil),
                      after,
                      title)

      case sa @ SlackAction.SelectedOption("suggest-showtime", showtimeJson) =>
        val timeFormater = DateTimeFormatter.ofPattern("EEEE h:mma")
        val decoded = io.circe.parser.decode[Showtime](showtimeJson)
        decoded.fold(
          _ => response("Invalid showtime"),
          showtime =>
            responseInChannel(
              s"${sa.user.mention} has suggested a movie time!\n${showtime.movieName}\n" +
                s"${showtime.start.atZone(PacificTime.tz).format(timeFormater)}\n${showtime.theaterName}.",
              attachment(
                "Can you make it?",
                "??",
                button("interested", "yes", Some("yes"), Style.Primary),
                button("interested", "no", Some("no"), Style.Danger))
          )
        )

      case sa @ SlackAction.SelectedOption("interested", answer) =>
        respondAndReplace(
          sa.originalMessage.map(_.text).getOrElse("oops... something went wrong..."),
          sa.originalAttachments :+ attachment(s"${sa.user.mention} says $answer", "1"):_*
        )

      case sa @ SlackAction.SelectedOption(cmd, arg) =>
        response(s"$cmd($arg)")

      case sa =>
        response(s"Unknown command ${sa.actions}")

    }
  }

  def listShowtimes(allShowtimes: Seq[Showtime],
                    movies: Seq[String],
                    theaters: Seq[String],
                    after: ZonedDateTime,
                    title: String = "Showtimes"): SlackMessage = {
    val timeFormater = DateTimeFormatter.ofPattern("h:mma")
    val maxActionsPerAttachmentInSlack = 5
    val showtimes = allShowtimes
      .filter { showtime =>
        (movies.isEmpty || movies.contains(showtime.movieName)) &&
        (theaters.isEmpty || theaters.contains(showtime.theaterName)) &&
        showtime.start.isAfter(after.toInstant) &&
        showtime.start.isBefore(
          after
            .plusDays(1)
            .toLocalDate
            .atStartOfDay(after.getOffset)
            .toInstant) &&
        showtime.start.isAfter(Instant.now())
      }
      .groupBy(s => (s.movieName, s.theaterName))
      .toList
      .sortBy(_._1)
    val matches = showtimes.flatMap {
      case ((movie, theater), times) =>
        times.sortBy(_.start).grouped(maxActionsPerAttachmentInSlack).map {
          shows5 =>
            attachment(
              s"$movie at $theater",
              movie,
              shows5.map { showtime =>
                button(
                  "suggest-showtime",
                  showtime.start.atOffset(after.getOffset).format(timeFormater),
                  Some(showtime.asJson.noSpaces))
              }: _*
            )
        }
    }

    if (matches.isEmpty) {
      respondAndReplace(
        "Sorry, but there were no showtimes matching your search.")
    } else
      respondAndReplace(title, matches: _*)
        .copy(responseType = Some(ResponseType.InChannel))
  }

  def listMovies(allShowtimes: Seq[Showtime]): SlackMessage = {
    response("All movies playing today",
             attachment(
               sortByOccurance(allShowtimes.map(_.movieName)).mkString("\n"),
               "123"
             ))
  }

  def listTheaters(allShowtimes: Seq[Showtime]): SlackMessage = {
    response("All theaters playing movies today",
             attachment(
               sortByOccurance(allShowtimes.map(_.theaterName)).mkString("\n"),
               "123"
             ))

  }

  private def readMultiselectForm(
      attachments: Seq[Attachment]): Map[String, Seq[String]] = {
    attachments.map { attachment =>
      val values = attachment.actions.getOrElse(Nil).collect {
        case Action(_,
                    _,
                    _,
                    _,
                    Some(Seq(SelectOption(text, value, _))),
                    _,
                    _) =>
          value
        case Action(_, text, _, value, _, _, None) =>
          value.filterNot(_.isEmpty).getOrElse(text)
      }
      attachment.callbackId.getOrElse("") -> values
    }.toMap
  }

  private def updateMultiselect(attachments: Seq[Attachment],
                                callbackId: String,
                                newValue: String): Seq[Attachment] = {
    attachments.map {
      case att if att.callbackId.contains(callbackId) =>
        val removeFromOptions = att.actions.getOrElse(Nil).map {
          case action @ Action(_, _, _, _, _, _, Some(oldOptions)) =>
            action.copy(
              options = Some(oldOptions.filterNot(_.value == newValue)))
          case other => other
        }
        val addAsButton = button(newValue, newValue)
        att.copy(actions = Some(removeFromOptions ++ Seq(addAsButton)))
      case other => other
    }
  }

  private def sortByOccurance[T](seq: Seq[T]) =
    seq
      .groupBy(identity)
      .toSeq
      .sortBy(_._2.length)(implicitly[Ordering[Int]].reverse)
      .map(_._1)

}
