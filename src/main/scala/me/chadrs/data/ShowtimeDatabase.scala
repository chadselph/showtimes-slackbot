package me.chadrs.data

import java.io.File
import java.nio.file.{Files, Path}
import java.time.LocalDate

import cats.effect.IO
import io.circe.jawn.parseFile

import scala.util.Try

trait ShowtimeDatabase {

  def getShowTimes(): IO[Seq[Showtime]]

}

object StaticShowtimes extends ShowtimeDatabase {
  override def getShowTimes(): IO[Seq[Showtime]] = {
    val file = new File(getClass.getResource("/showtimes-2018-05-01.json").toURI)
    IO.pure(parseFile(file).flatMap(_.as[List[Showtime]]).right.get)
  }
}


// I didn't want to write this but EHCache has persistence as an enterprise-only feaure >:-o and mapdb looked awful
// also I didn't want to write sql for something so simple
class DailyCachedShowtimes(db: ShowtimeDatabase, persistence: Option[Path]) extends ShowtimeDatabase {

  persistence.foreach { p =>
    if (!p.toFile.exists()) {
      p.toFile.mkdirs()
    }
  }

  private def path(date: LocalDate): Option[Path] = persistence.map(_.resolve(date.toString))

  private def persist(date: LocalDate, values: Seq[Showtime]): Unit = {
    path(date).foreach { path =>
      import java.nio.file.Files
      import io.circe.syntax._
      val writeFile = Try {
        val newFile = Files.createFile(path)
        Files.write(newFile, values.asJson.spaces2.getBytes("UTF-8"))
      }
      println(s"writing file was: $writeFile")
    }
  }

  private def readDisk(date: LocalDate): Option[Seq[Showtime]] = {
    path(date).flatMap { p =>
      if (Files.exists(p)) {
        io.circe.jawn.parseFile(p.toFile).flatMap(_.as[Seq[Showtime]]).toOption
      } else None
    }
  }

  private def writeToMem(date: LocalDate, values: Seq[Showtime]): Seq[Showtime] = {
    memory(date) = values
    values
  }

  private val memory = scala.collection.concurrent.TrieMap[LocalDate, Seq[Showtime]]()

  override def getShowTimes(): IO[Seq[Showtime]] = synchronized {
    val date = LocalDate.now()
    memory.get(date).orElse(readDisk(date).map(writeToMem(date, _))) match {
      case None =>
        val value = db.getShowTimes()
        value.map { contents =>
          writeToMem(date, contents)
          persist(date, contents)
          contents
        }
      case Some(value) => IO.pure(value)
    }
  }
}

