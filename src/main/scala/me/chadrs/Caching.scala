package me.chadrs

import java.io.{InputStream, OutputStream}
import java.nio.file.{Path, Paths}
import java.time.LocalDate

import cats.effect.IO
import me.chadrs.cachelayers.{CirceFileCache, CirceS3Cache, InMemoryCache, WriteThroughCache}
import me.chadrs.data.{Showtime, ShowtimeDatabase}
import me.chadrs.moviescan.clients.{TmsMovieSearch, TmsShowtimeDatabase}
import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.s3.S3AsyncClient

object Caching {

  val BucketName: String = "movie-showtimes"
  val FileDir: Path = Paths.get("showtimes-cache")

  def s3BackedCache(bucket: String, db: ShowtimeDatabase)
    : WriteThroughCache[IO, (String, LocalDate), Seq[Showtime]] =
    new InMemoryCache[IO, (String, LocalDate), Seq[Showtime]](
      new CirceS3Cache[(String, LocalDate), Seq[Showtime]](
        S3AsyncClient.create(),
        bucket,
        { case (zip, localdate) => s"$localdate/$zip" },
        { case (zip, localdate) => db.getShowTimes(zip, localdate) })
    )

  def fileBackedCache(folder: Path, db: ShowtimeDatabase)
    : WriteThroughCache[IO, (String, LocalDate), Seq[Showtime]] =
    new InMemoryCache[IO, (String, LocalDate), Seq[Showtime]](
      new CirceFileCache[IO, (String, LocalDate), Seq[Showtime]]({
        case (zip, localdate) =>
          folder.resolve(Paths.get(localdate.toString, zip))
      }, { case (zip, localdate) => db.getShowTimes(zip, localdate) })
    )


}

// Lambda cron tasks
class CacheTasks {

  private val logger = LoggerFactory.getLogger(getClass)

  def loadCache(_is: InputStream, os: OutputStream): Unit = {
    val start = System.currentTimeMillis()
    Caching.s3BackedCache(Caching.BucketName, new TmsShowtimeDatabase(TmsMovieSearch.fromEnv()))
        .getOrUpdate(("94114", PacificTime.today())).unsafeRunSync()
    os.write("""{"success": true}""".getBytes)
    logger.info(s"Ran cache update in ${System.currentTimeMillis() - start}ms")
  }

}
