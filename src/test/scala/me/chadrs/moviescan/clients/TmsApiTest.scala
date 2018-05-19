package me.chadrs.moviescan.clients

import java.io.File

import me.chadrs.moviescan.clients.Tms.MovieShowing
import io.circe.jawn.parseFile
import org.specs2.mutable.Specification

class TmsApiTest extends Specification {

  val testData = new File(getClass.getResource("/tms-resp.json").toURI)

  "tms json parser" >> {
    "should not fail when parsing json file" >> {

      parseFile(testData).flatMap(_.as[Seq[MovieShowing]]).toTry.get.size shouldEqual 40
    }
  }


}
