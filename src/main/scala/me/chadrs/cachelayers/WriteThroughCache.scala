package me.chadrs.cachelayers

import java.nio.file.{Files, Path}
import java.util.concurrent.{CancellationException, CompletableFuture, CompletionException}

import cats.Monad
import cats.effect.IO
import io.circe.{Decoder, Encoder}
import software.amazon.awssdk.core.async.{AsyncRequestProvider, AsyncResponseHandler}
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.{GetObjectRequest, GetObjectResponse, PutObjectRequest}

import scala.util.Try

trait WriteThroughCache[F[_], K, V] extends ((K) => F[V]) {
  def getOrUpdate(key: K): F[V]
  override def apply(key: K): F[V] = getOrUpdate(key)
}

abstract class BackedWriteThroughCache[F[_]: Monad, K, V](backing: K => F[V]) extends  WriteThroughCache[F, K ,V] {

  protected def get(key: K): Option[V]
  protected def set(key: K, value: V): F[Unit]

  final def getOrUpdate(key: K): F[V] = {
    this
      .get(key)
      .fold {
        val value = backing(key)
        implicitly[Monad[F]].flatMap(value) { v =>
          implicitly[Monad[F]].map(this.set(key, v))(_ => v)
        }
      } {
        implicitly[Monad[F]].pure
      }
  }

}

class InMemoryCache[F[_]: Monad, K, V](backing: K => F[V])
    extends BackedWriteThroughCache[F, K, V](backing) {

  private val cache = scala.collection.concurrent.TrieMap[K, V]()
  override protected def get(key: K): Option[V] = cache.get(key)
  override protected def set(key: K, value: V): F[Unit] =
    implicitly[Monad[F]].pure(cache.put(key, value))
}

class CirceFileCache[F[_]: Monad, K, V: Encoder: Decoder](getPath: K => Path,
                                                          backing: K => F[V])
    extends BackedWriteThroughCache[F, K, V](backing) {

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

class CirceS3Cache[K, V: Encoder: Decoder](s3Client: S3AsyncClient,
                                                        bucket: String,
                                                        getKey: K => String,
                                                        backing: K => IO[V])
    extends WriteThroughCache[IO, K, V] {

  private def fromJavaFuture[A](cf: CompletableFuture[A]): IO[A] =
    IO.cancelable(cb => {
      cf.handle[Unit]((result: A, err: Throwable) => {
        err match {
          case null =>
            cb(Right(result))
          case _: CancellationException =>
            ()
          case ex: CompletionException if ex.getCause ne null =>
            cb(Left(ex.getCause))
          case ex =>
            cb(Left(ex))
        }
      })
      IO(cf.cancel(true))
    })

  override def getOrUpdate(key: K): IO[V] = {
    val s3Key = getKey(key)
    val req = GetObjectRequest.builder().bucket(bucket).key(s3Key).build()
    fromJavaFuture(
      s3Client.getObject(req,
                         AsyncResponseHandler.toUtf8String[GetObjectResponse]))
      .attempt
      .map(str => str.flatMap(io.circe.jawn.parse).flatMap(_.as[V]))
      .flatMap {
        case Left(_) =>
          backing(key).map { value =>
            val req = PutObjectRequest.builder().bucket(bucket).key(s3Key).build()
            val obj = io.circe.Encoder[V].apply(value).spaces2
            s3Client.putObject(req, AsyncRequestProvider.fromString(obj))
            value
          }
        case Right(v) => IO.pure(v)
      }
  }
}
