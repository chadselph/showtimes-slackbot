package me.chadrs.moviepoll
import collection.immutable.Seq

case class MovieShowtimesPoll(movieAndTheaters: Seq[MovieAndTheaterPollCategory]) {

  def vote(uid: String, voter: String): MovieShowtimesPoll =
    voteOrUnvote(uid, voter, _ + _)

  def unvote(uid: String, voter: String): MovieShowtimesPoll =
    voteOrUnvote(uid, voter, _ - _)

  private def voteOrUnvote(uid: String, voter: String, op: (Set[String], String) => Set[String]): MovieShowtimesPoll = {
    this.copy(movieAndTheaters.map { movieAndTheater =>
      movieAndTheater.copy(
        showtimes = movieAndTheater.showtimes.map {
          case showtime if showtime.uid == uid => showtime.copy(votes = showtime.votes + voter)
          case other => other
        }
      )
    })
  }
}

case class MovieAndTheaterPollCategory(movie: String,
                                       theater: String,
                                       showtimes: Seq[MovieShowtimePollChoice])

case class MovieShowtimePollChoice(time: String,
                                   uid: String,
                                   votes: Set[String])
