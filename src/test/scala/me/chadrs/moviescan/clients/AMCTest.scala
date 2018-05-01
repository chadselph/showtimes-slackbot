package me.chadrs.moviescan.clients

import java.io.File
import java.time.Instant

import cats.effect.IO
import org.specs2.mutable.Specification
import org.http4s.client.blaze._
import io.circe.literal._
import me.chadrs.data.Showtime
// import org.http4s.client.blaze._


class AMCTest extends Specification {

  val json = io.circe.jawn
    .parseFile(new File(getClass.getResource("/amc-gql.json").toURI))
    .getOrElse(throw new Exception("no json found!!"))
  val amcClient = new AMC(Http1Client[IO]().unsafeRunSync)

  "AMC json parser" >> {

    "should parse one movie" >> {
      val json = json"""
     {
       "data": {
         "viewer": {
           "user": {
             "vanness": {
               "edges": [
                 {
                   "_movie": {
                     "name": "Black Panther",
                     "mpaaRating": "PG13",
                     "runTime": 134,
                     "ratings": {
                       "rottenTomatoes": {
                         "criticsScore": 98
                       }
                     },
                     "theatres": {
                       "edges": [
                         {
                           "_theatre": {
                             "name": "AMC Van Ness 14",
                             "formats": {
                               "items": [
                                 {
                                   "attributes": [],
                                   "groups": {
                                     "edges": [
                                       {
                                         "node": {
                                           "showtimes": {
                                             "edges": [
                                               {
                                                 "_showtime": {
                                                   "when": "2018-02-16T03:00:00.000Z",
                                                   "isSoldOut": true,
                                                   "isAlmostSoldOut": false
                                                 }
                                               }
                                             ]
                                           }
                                         }
                                       }
                                     ]
                                   }
                                 }
                               ]
                           }
                         }
                         }
                       ]
                     }
                   }
                 }
               ]
             }
           }
         }
       }
     } """
      amcClient.decodeGraphJsonToShowTimes(json) must containTheSameElementsAs(List(
        Showtime("Black Panther", Instant.parse("2018-02-16T03:00:00.000Z"), "AMC Van Ness 14", Set(), Some("PG13"), 134, Some(98))
      ))

    }

    "should parse a sample file" >> {
      implicit def string2instant(s: String): Instant = Instant.parse(s)

      val list = amcClient.decodeGraphJsonToShowTimes(json)
      list.size must equalTo (116)
      list must containAllOf(List(
          Showtime("Black Panther","2018-02-16T03:00:00Z","AMC Van Ness 14",Set(),Some("PG13"),134,None),
          Showtime("Black Panther","2018-02-16T06:30:00Z","AMC Van Ness 14",Set(),Some("PG13"),134,None),
          Showtime("Black Panther","2018-02-16T03:00:00Z","AMC Van Ness 14",Set(),Some("PG13"),134,None),
          Showtime("Black Panther","2018-02-16T06:45:00Z","AMC Van Ness 14",Set(),Some("PG13"),134,None),
          Showtime("Black Panther","2018-02-16T03:00:00Z","AMC Van Ness 14",Set(),Some("PG13"),134,None),
          Showtime("Black Panther","2018-02-16T06:00:00Z","AMC Van Ness 14",Set(),Some("PG13"),134,None),
          Showtime("Fifty Shades Freed","2018-02-15T19:55:00Z","AMC Van Ness 14",Set(),Some("R"),101,None),
          Showtime("Fifty Shades Freed","2018-02-15T22:00:00Z","AMC Van Ness 14",Set(),Some("R"),101,None),
          Showtime("Fifty Shades Freed","2018-02-16T00:45:00Z","AMC Van Ness 14",Set(),Some("R"),101,None),
          Showtime("Fifty Shades Freed","2018-02-16T03:30:00Z","AMC Van Ness 14",Set(),Some("R"),101,None),
          Showtime("Fifty Shades Freed","2018-02-16T06:15:00Z","AMC Van Ness 14",Set(),Some("R"),101,None),
          Showtime("Fifty Shades Freed","2018-02-15T20:30:00Z","AMC Van Ness 14",Set(),Some("R"),101,None),
          Showtime("Fifty Shades Freed","2018-02-15T23:00:00Z","AMC Van Ness 14",Set(),Some("R"),101,None),
          Showtime("Peter Rabbit","2018-02-15T19:55:00Z","AMC Van Ness 14",Set(),Some("PG"),94,None),
          Showtime("Peter Rabbit","2018-02-15T22:30:00Z","AMC Van Ness 14",Set(),Some("PG"),94,None),
          Showtime("Peter Rabbit","2018-02-16T01:00:00Z","AMC Van Ness 14",Set(),Some("PG"),94,None),
          Showtime("Peter Rabbit","2018-02-16T03:35:00Z","AMC Van Ness 14",Set(),Some("PG"),94,None),
          Showtime("Peter Rabbit","2018-02-16T06:05:00Z","AMC Van Ness 14",Set(),Some("PG"),94,None),
          Showtime("Jumanji: Welcome To The Jungle","2018-02-15T19:55:00Z","AMC Van Ness 14",Set(),Some("PG13"),119,Some(76)),
          Showtime("Jumanji: Welcome To The Jungle","2018-02-15T22:45:00Z","AMC Van Ness 14",Set(),Some("PG13"),119,Some(76)),
          Showtime("Jumanji: Welcome To The Jungle","2018-02-16T06:45:00Z","AMC Van Ness 14",Set(),Some("PG13"),119,Some(76)),
          Showtime("The 15:17 To Paris","2018-02-15T21:25:00Z","AMC Van Ness 14",Set(),Some("PG13"),99,None),
          Showtime("The 15:17 To Paris","2018-02-16T00:15:00Z","AMC Van Ness 14",Set(),Some("PG13"),99,None),
          Showtime("The 15:17 To Paris","2018-02-16T02:50:00Z","AMC Van Ness 14",Set(),Some("PG13"),99,None),
          Showtime("The 15:17 To Paris","2018-02-16T06:35:00Z","AMC Van Ness 14",Set(),Some("PG13"),99,None),
          Showtime("Early Man","2018-02-16T01:00:00Z","AMC Van Ness 14",Set(),Some("PG"),89,None),
          Showtime("Early Man","2018-02-16T03:30:00Z","AMC Van Ness 14",Set(),Some("PG"),89,None),
          Showtime("Early Man","2018-02-16T06:05:00Z","AMC Van Ness 14",Set(),Some("PG"),89,None),
          Showtime("The Maze Runner: The Death Cure","2018-02-15T19:55:00Z","AMC Van Ness 14",Set(),Some("PG13"),141,None),
          Showtime("The Maze Runner: The Death Cure","2018-02-15T23:05:00Z","AMC Van Ness 14",Set(),Some("PG13"),141,None),
          Showtime("Samson","2018-02-16T03:00:00Z","AMC Van Ness 14",Set(),Some("PG13"),110,None),
          Showtime("Samson","2018-02-16T06:00:00Z","AMC Van Ness 14",Set(),Some("PG13"),110,None),
          Showtime("The Post","2018-02-16T06:40:00Z","AMC Van Ness 14",Set(),Some("PG13"),116,Some(88)),
          Showtime("The Post","2018-02-15T21:30:00Z","AMC Van Ness 14",Set(),Some("PG13"),116,Some(88)),
          Showtime("Winchester","2018-02-15T19:55:00Z","AMC Van Ness 14",Set(),Some("PG13"),99,None),
          Showtime("Winchester","2018-02-15T22:25:00Z","AMC Van Ness 14",Set(),Some("PG13"),99,None),
          Showtime("The Shape Of Water","2018-02-15T21:15:00Z","AMC Van Ness 14",Set(),Some("R"),123,Some(92)),
          Showtime("The Shape Of Water","2018-02-16T00:15:00Z","AMC Van Ness 14",Set(),Some("R"),123,Some(92)),
          Showtime("The Shape Of Water","2018-02-16T03:15:00Z","AMC Van Ness 14",Set(),Some("R"),123,Some(92)),
          Showtime("The Shape Of Water","2018-02-16T06:15:00Z","AMC Van Ness 14",Set(),Some("R"),123,Some(92)),
          Showtime("Coco","2018-02-15T19:55:00Z","AMC Van Ness 14",Set(),Some("PG"),105,Some(97)),
          Showtime("Coco","2018-02-15T22:30:00Z","AMC Van Ness 14",Set(),Some("PG"),105,Some(97)),
          Showtime("I, Tonya","2018-02-15T21:35:00Z","AMC Van Ness 14",Set(),Some("R"),120,Some(90)),
          Showtime("I, Tonya","2018-02-16T00:35:00Z","AMC Van Ness 14",Set(),Some("R"),120,Some(90)),
          Showtime("I, Tonya","2018-02-16T03:35:00Z","AMC Van Ness 14",Set(),Some("R"),120,Some(90)),
          Showtime("I, Tonya","2018-02-16T06:35:00Z","AMC Van Ness 14",Set(),Some("R"),120,Some(90)),
          Showtime("Padmaavat","2018-02-15T22:30:00Z","AMC Van Ness 14",Set(),Some("PG13"),164,None),
          Showtime("Phantom Thread","2018-02-16T00:00:00Z","AMC Van Ness 14",Set(),Some("R"),130,None),
          Showtime("Phantom Thread","2018-02-15T21:00:00Z","AMC Van Ness 14",Set(),Some("R"),130,None),
          Showtime("Call Me By Your Name","2018-02-15T20:50:00Z","AMC Van Ness 14",Set(),Some("R"),130,Some(96)),
          Showtime("Call Me By Your Name","2018-02-16T00:00:00Z","AMC Van Ness 14",Set(),Some("R"),130,Some(96)),
          Showtime("Call Me By Your Name","2018-02-16T03:05:00Z","AMC Van Ness 14",Set(),Some("R"),130,Some(96)),
          Showtime("Call Me By Your Name","2018-02-16T06:20:00Z","AMC Van Ness 14",Set(),Some("R"),130,Some(96)),
          Showtime("Permission","2018-02-16T03:45:00Z","AMC Van Ness 14",Set(),Some("NR"),96,None),
          Showtime("Permission","2018-02-16T06:25:00Z","AMC Van Ness 14",Set(),Some("NR"),96,None),
          Showtime("Black Panther","2018-02-16T03:15:00Z","AMC Metreon 16",Set(),Some("PG13"),134,None),
          Showtime("Black Panther","2018-02-16T04:00:00Z","AMC Metreon 16",Set(),Some("PG13"),134,None),
          Showtime("Black Panther","2018-02-16T04:30:00Z","AMC Metreon 16",Set(),Some("PG13"),134,None),
          Showtime("Black Panther","2018-02-16T05:30:00Z","AMC Metreon 16",Set(),Some("PG13"),134,None),
          Showtime("Black Panther","2018-02-16T06:30:00Z","AMC Metreon 16",Set(),Some("PG13"),134,None),
          Showtime("Black Panther","2018-02-16T06:45:00Z","AMC Metreon 16",Set(),Some("PG13"),134,None),
          Showtime("Black Panther","2018-02-16T07:00:00Z","AMC Metreon 16",Set(),Some("PG13"),134,None),
          Showtime("Black Panther","2018-02-16T03:30:00Z","AMC Metreon 16",Set(),Some("PG13"),134,None),
          Showtime("Black Panther","2018-02-16T07:00:00Z","AMC Metreon 16",Set(),Some("PG13"),134,None),
          Showtime("Black Panther","2018-02-16T03:00:00Z","AMC Metreon 16",Set(),Some("PG13"),134,None),
          Showtime("Black Panther","2018-02-16T06:30:00Z","AMC Metreon 16",Set(),Some("PG13"),134,None),
          Showtime("Black Panther","2018-02-16T03:30:00Z","AMC Metreon 16",Set(),Some("PG13"),134,None),
          Showtime("Black Panther","2018-02-16T06:15:00Z","AMC Metreon 16",Set(),Some("PG13"),134,None),
          Showtime("Black Panther","2018-02-16T06:30:00Z","AMC Metreon 16",Set(),Some("PG13"),134,None),
          Showtime("Black Panther","2018-02-16T07:15:00Z","AMC Metreon 16",Set(),Some("PG13"),134,None),
          Showtime("Black Panther","2018-02-16T04:00:00Z","AMC Metreon 16",Set(),Some("PG13"),134,None),
          Showtime("Black Panther","2018-02-16T05:00:00Z","AMC Metreon 16",Set(),Some("PG13"),134,None),
          Showtime("Black Panther","2018-02-16T05:15:00Z","AMC Metreon 16",Set(),Some("PG13"),134,None),
          Showtime("Opening Night Fan Event: Black Panther","2018-02-16T02:00:00Z","AMC Metreon 16",Set(),Some("PG13"),134,None),
          Showtime("Fifty Shades Freed","2018-02-16T06:10:00Z","AMC Metreon 16",Set(),Some("R"),101,None),
          Showtime("Fifty Shades Freed","2018-02-15T21:45:00Z","AMC Metreon 16",Set(),Some("R"),101,None),
          Showtime("Fifty Shades Freed","2018-02-16T00:15:00Z","AMC Metreon 16",Set(),Some("R"),101,None),
          Showtime("Fifty Shades Freed","2018-02-16T02:45:00Z","AMC Metreon 16",Set(),Some("R"),101,None),
          Showtime("Fifty Shades Freed","2018-02-16T06:30:00Z","AMC Metreon 16",Set(),Some("R"),101,None),
          Showtime("Fifty Shades Freed","2018-02-15T19:15:00Z","AMC Metreon 16",Set(),Some("R"),101,None),
          Showtime("Fifty Shades Freed","2018-02-15T22:00:00Z","AMC Metreon 16",Set(),Some("R"),101,None),
          Showtime("Fifty Shades Freed","2018-02-16T00:45:00Z","AMC Metreon 16",Set(),Some("R"),101,None),
          Showtime("Fifty Shades Freed","2018-02-15T21:00:00Z","AMC Metreon 16",Set(),Some("R"),101,None),
          Showtime("Fifty Shades Freed","2018-02-15T23:45:00Z","AMC Metreon 16",Set(),Some("R"),101,None),
          Showtime("Peter Rabbit","2018-02-15T19:00:00Z","AMC Metreon 16",Set(),Some("PG"),94,None),
          Showtime("Peter Rabbit","2018-02-15T21:30:00Z","AMC Metreon 16",Set(),Some("PG"),94,None),
          Showtime("Peter Rabbit","2018-02-16T02:15:00Z","AMC Metreon 16",Set(),Some("PG"),94,None),
          Showtime("Jumanji: Welcome To The Jungle","2018-02-15T22:00:00Z","AMC Metreon 16",Set(),Some("PG13"),119,Some(76)),
          Showtime("Jumanji: Welcome To The Jungle","2018-02-15T19:10:00Z","AMC Metreon 16",Set(),Some("PG13"),119,Some(76)),
          Showtime("Jumanji: Welcome To The Jungle","2018-02-16T00:40:00Z","AMC Metreon 16",Set(),Some("PG13"),119,Some(76)),
          Showtime("The Greatest Showman","2018-02-15T20:30:00Z","AMC Metreon 16",Set(),Some("PG"),105,None),
          Showtime("The Greatest Showman","2018-02-15T23:10:00Z","AMC Metreon 16",Set(),Some("PG"),105,None),
          Showtime("Early Man","2018-02-16T01:00:00Z","AMC Metreon 16",Set(),Some("PG"),89,None),
          Showtime("The Maze Runner: The Death Cure","2018-02-15T19:15:00Z","AMC Metreon 16",Set(),Some("PG13"),141,None),
          Showtime("The Maze Runner: The Death Cure","2018-02-15T22:30:00Z","AMC Metreon 16",Set(),Some("PG13"),141,None),
          Showtime("The Post","2018-02-15T19:10:00Z","AMC Metreon 16",Set(),Some("PG13"),116,Some(88)),
          Showtime("The Post","2018-02-15T22:15:00Z","AMC Metreon 16",Set(),Some("PG13"),116,Some(88)),
          Showtime("The Post","2018-02-16T01:00:00Z","AMC Metreon 16",Set(),Some("PG13"),116,Some(88)),
          Showtime("Den Of Thieves","2018-02-15T19:30:00Z","AMC Metreon 16",Set(),Some("R"),140,None),
          Showtime("Den Of Thieves","2018-02-15T22:45:00Z","AMC Metreon 16",Set(),Some("R"),140,None),
          Showtime("Winchester","2018-02-15T20:15:00Z","AMC Metreon 16",Set(),Some("PG13"),99,None),
          Showtime("Winchester","2018-02-15T22:50:00Z","AMC Metreon 16",Set(),Some("PG13"),99,None),
          Showtime("Winchester","2018-02-16T01:30:00Z","AMC Metreon 16",Set(),Some("PG13"),99,None),
          Showtime("12 Strong","2018-02-15T20:05:00Z","AMC Metreon 16",Set(),Some("R"),130,None),
          Showtime("Hostiles","2018-02-15T23:05:00Z","AMC Metreon 16",Set(),Some("R"),135,None),
          Showtime("Darkest Hour","2018-02-15T20:45:00Z","AMC Metreon 16",Set(),Some("PG13"),124,Some(86)),
          Showtime("Darkest Hour","2018-02-15T23:45:00Z","AMC Metreon 16",Set(),Some("PG13"),124,Some(86)),
          Showtime("Star Wars: The Last Jedi","2018-02-15T21:15:00Z","AMC Metreon 16",Set(),Some("PG13"),152,Some(91)),
          Showtime("Star Wars: The Last Jedi","2018-02-16T00:45:00Z","AMC Metreon 16",Set(),Some("PG13"),152,Some(91)),
          Showtime("Paddington 2","2018-02-15T19:45:00Z","AMC Metreon 16",Set(),Some("PG"),103,Some(100)),
          Showtime("Paddington 2","2018-02-15T22:25:00Z","AMC Metreon 16",Set(),Some("PG"),103,Some(100)),
          Showtime("Molly's Game","2018-02-15T19:00:00Z","AMC Metreon 16",Set(),Some("R"),140,Some(82)),
          Showtime("The Commuter","2018-02-16T00:00:00Z","AMC Metreon 16",Set(),Some("PG13"),105,None),
          Showtime("Dunkirk","2018-02-15T23:15:00Z","AMC Metreon 16",Set(),Some("PG13"),106,Some(92)),
          Showtime("Monster Hunt 2","2018-02-16T03:00:00Z","AMC Metreon 16",Set(),Some("NR"),119,None),
          Showtime("Monster Hunt 2","2018-02-16T06:15:00Z","AMC Metreon 16",Set(),Some("NR"),119,None)
        )
      )
    }
  }

}
