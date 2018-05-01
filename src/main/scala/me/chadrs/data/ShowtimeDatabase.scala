package me.chadrs.data

import java.io.File
import io.circe.jawn.parseFile

trait ShowtimeDatabase {

  def getShowTimes(): Seq[Showtime]

}

object StaticShowtimes extends ShowtimeDatabase {
  override def getShowTimes(): Seq[Showtime] = {
    val file = new File(getClass.getResource("/showtimes-2018-05-01.json").toURI)
    parseFile(file).flatMap(_.as[List[Showtime]]).right.get
  }
}


