package me.chadrs.data

import java.io.File
import java.nio.file.Path
import java.time.LocalDate

import cats.effect.IO
import io.circe.jawn.parseFile
import me.chadrs.cachelayers.{CirceFileCache, InMemoryCache}

trait ShowtimeDatabase {

  def getShowTimes(zip: String, date: LocalDate): IO[Seq[Showtime]]

}

object StaticShowtimes extends ShowtimeDatabase {
  override def getShowTimes(zip: String, date: LocalDate): IO[Seq[Showtime]] = {
    val file = new File(getClass.getResource("/showtimes-2018-05-01.json").toURI)
    IO.pure(parseFile(file).flatMap(_.as[List[Showtime]]).right.get)
  }
}


// I didn't want to write this but EHCache has persistence as an enterprise-only feaure >:-o and mapdb looked awful
// also I didn't want to write sql for something so simple
class DailyCachedShowtimes(db: ShowtimeDatabase, persistence: Option[Path]) extends ShowtimeDatabase {

  val cache = persistence.fold {
    new InMemoryCache[IO, (String, LocalDate), Seq[Showtime]]((db.getShowTimes _).tupled)
  } { dir =>
    new InMemoryCache[IO, (String, LocalDate), Seq[Showtime]](
      new CirceFileCache[IO, (String, LocalDate), Seq[Showtime]] (
        { case (zip, date) => dir.resolve(s"$date-$zip") },
        (db.getShowTimes _).tupled
      ))
  }

  override def getShowTimes(zip: String, date: LocalDate): IO[Seq[Showtime]] = synchronized {
    cache.getOrUpdate((zip, date))
  }
}

