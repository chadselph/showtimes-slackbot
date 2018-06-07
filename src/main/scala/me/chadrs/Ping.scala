package me.chadrs

import java.io.{InputStream, OutputStream}

class Ping {
  def ping(_is: InputStream, os: OutputStream): Unit = {
    os.write("\"pong\"".getBytes)
  }

}
