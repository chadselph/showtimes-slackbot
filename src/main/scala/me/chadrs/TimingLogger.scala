package me.chadrs

import org.slf4j.LoggerFactory

object TimingLogger {

  private val logger = LoggerFactory.getLogger(getClass)

  def time[A](name: String)(code: => A): A = {
    val start = System.currentTimeMillis()
    val result = code
    logger.info(s"${System.currentTimeMillis() - start}ms - $name")
    result
  }

}
