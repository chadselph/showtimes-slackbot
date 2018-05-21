package me.chadrs.cachelayers

import java.nio.file.{Files, Path}

import cats.Monad
import io.circe.{Decoder, Encoder}

import scala.util.Try

abstract class WriteThroughCache[F[_]: Monad, K, V](backing: K => F[V]) extends ((K) => F[V]) {

  protected def get(key: K): Option[V]
  protected def set(key: K, value: V): F[Unit]

  final def getOrUpdate(key: K): F[V] = {
    this
      .get(key)
      .fold {
        val value = backing(key)
        implicitly[Monad[F]].map(value) { v =>
          this.set(key, v) // not waiting on this to complete, but we could if we used flatMap and another map
          v
        }
      } {
        implicitly[Monad[F]].pure
      }
  }

  override def apply(key: K): F[V] = getOrUpdate(key)

}

class InMemoryCache[F[_]: Monad, K, V](backing: K => F[V])
    extends WriteThroughCache[F, K, V](backing) {

  private val cache = scala.collection.concurrent.TrieMap[K, V]()
  override protected def get(key: K): Option[V] = cache.get(key)
  override protected def set(key: K, value: V): F[Unit] =
    implicitly[Monad[F]].pure(cache.put(key, value))
}

class CirceFileCache[F[_]: Monad, K, V: Encoder: Decoder](
    getPath: K => Path,
    backing: K => F[V])
    extends WriteThroughCache[F, K, V](backing) {

  override protected def get(key: K): Option[V] = {
    val p = getPath(key)
    if (Files.exists(p)) {
      io.circe.jawn.parseFile(p.toFile).flatMap(_.as[V]).toOption
    } else None
  }

  override def set(date: K, values: V): F[Unit] = {
    val path = getPath(date)
    import io.circe.syntax._
    val writeFile = Try {
      path.getParent.toFile.mkdirs()
      val newFile = Files.createFile(path)
      Files.write(newFile, values.asJson.spaces2.getBytes("UTF-8"))
    }
    println(s"writing file was: $writeFile")
    implicitly[Monad[F]].unit
  }
}
