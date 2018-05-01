package me.chadrs.moviepoll

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalTime, ZoneOffset, ZonedDateTime}

import me.chadrs.data.StaticShowtimes
import me.chadrs.slack.SlackAction
import me.chadrs.slack.SlackMessageBuilder.SlackMessage
import me.chadrs.slack.SlackMessageBuilder._

object ShowtimesPoll {

  def newPoll(): SlackMessage = {
    val movies = StaticShowtimes
      .getShowTimes()
      .map(_.movieName)
      .distinct
      .map(option(_))
    val theaters = StaticShowtimes
      .getShowTimes()
      .map(_.theaterName)
      .distinct
      .map(option(_))
    val hours =
      select("time", "time", 1.to(10).map(h => option(s"${h}pm")): _*)
    val days =
      List("Today", "Tomorrow", "Day after tomorrow").zipWithIndex.map {
        case (s, index) =>
          button("create", s, Style.Primary)
            .copy(value = Some(LocalDate.now().plusDays(index).toString))
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

  def update(input: SlackAction): SlackMessage = {
    input match {
      case sa @ SlackAction.SelectedOption("movie", movie) =>
        respondAndReplace(
          s"Added $movie",
          updateMultiselect(sa.originalMessage.attachments, "movie", movie): _*
        )
      case sa @ SlackAction.SelectedOption("theater", theater) =>
        respondAndReplace(s"Added $theater",
                          updateMultiselect(sa.originalMessage.attachments,
                                            "theater",
                                            theater): _*)

      case sa @ SlackAction.SelectedOption("time", time) =>
        val updatedAttachments = sa.originalMessage.attachments.map {
          case att if att.callbackId == "time" =>
            att.copy(
              actions = att.actions.map(
                _.copy(selectedOptions = Some(Seq(
                  SelectOption(time, time, None)
                )))))
          case other => other
        }
        respondAndReplace(s"set time to after $time", updatedAttachments: _*)

      case sa @ SlackAction.SelectedOption("create", day) =>
        val form = readMultiselectForm(sa.originalMessage.attachments)
        val simpleHourFormat = DateTimeFormatter.ofPattern("ha")
        def parseHour(hour: String): LocalTime = LocalTime.parse(hour.toUpperCase, simpleHourFormat)
        listShowtimes(form.getOrElse("movie", Nil), form.getOrElse("theater", Nil), LocalDate.parse(day), parseHour(form.get("time").flatMap(_.headOption).getOrElse("4pm")))

      case sa @ SlackAction.SelectedOption(cmd, arg) =>
        response(s"$cmd($arg)")

      case sa =>
        response(s"Unknown command ${sa.actions}")

    }
  }

  def listShowtimes(movies: Seq[String], theaters: Seq[String], day: LocalDate, after: LocalTime): SlackMessage = {
    val timeFormater = DateTimeFormatter.ofPattern("h:mma")
    val localOffset = ZonedDateTime.now.getOffset
    val maxActionsPerAttachmentInSlack = 5
    val showtimes = StaticShowtimes.getShowTimes().filter { showtime =>
      movies.contains(showtime.movieName) && theaters.contains(showtime.theaterName)
      /* && showtime.start.isAfter(
        day.atTime(after).toInstant(localOffset)
      ) && showtime.start.isBefore(day.plusDays(1).atTime(0, 0).toInstant(localOffset)) */
    }.groupBy(s => (s.movieName, s.theaterName)).toList.sortBy(_._1)

    respondAndReplace("Showtimes", showtimes.flatMap { case ((movie, theater), times) =>
      times.sortBy(_.start).grouped(maxActionsPerAttachmentInSlack).map { shows5 =>
        attachment(s"$movie at $theater", movie, shows5.map { showtime =>
          button(showtime.toString, showtime.start.atOffset(localOffset).format(timeFormater))
        }: _*)
      }
    }: _*)
  }

  private def readMultiselectForm(attachments: Seq[Attachment]): Map[String, Seq[String]] = {
    attachments.map { attachment =>
      val values = attachment.actions.collect {
        case Action(_, _, _, _, Some(Seq(SelectOption(text, value, _))), _, _) => value
        case Action(_, text, _, value, _, _, None) => value.filterNot(_.isEmpty).getOrElse(text)
      }
      attachment.callbackId -> values
    }.toMap
  }

  private def updateMultiselect(attachments: Seq[Attachment],
                                callbackId: String,
                                newValue: String): Seq[Attachment] = {
    attachments.map {
      case att if att.callbackId == callbackId =>
        val removeFromOptions = att.actions.map {
          case action @ Action(_, _, _, _, _, _, Some(oldOptions)) =>
            action.copy(
              options = Some(oldOptions.filterNot(_.value == newValue)))
          case other => other
        }
        val addAsButton = button(newValue, newValue)
        att.copy(actions = removeFromOptions ++ Seq(addAsButton))
      case other => other
    }
  }

}
