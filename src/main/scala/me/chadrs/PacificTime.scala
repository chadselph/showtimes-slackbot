package me.chadrs

import java.time.{LocalDate, LocalTime}

object PacificTime {
  private val tz = java.time.ZoneId.of("America/Los_Angeles")
  def today(): LocalDate = LocalDate.now(tz)
  def time(): LocalTime = LocalTime.now(tz)
}
