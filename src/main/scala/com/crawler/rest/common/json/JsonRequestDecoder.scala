package com.crawler.rest.common.json

import cats.MonadError
import cats.syntax.all._
import io.circe.{Decoder, Json}
import org.http4s.Request
import org.http4s.circe._

final class JsonRequestDecoder[
    F[_]: MonadError[*[_], Throwable]: JsonDecoder,
    A
](implicit decoder: Decoder[A]) {
  def decodeRequest(
      request: Request[F]
  ): F[A] =
    request.asJson.flatMap(decodeJson)

  private def decodeJson(
      json: Json
  )(implicit F: MonadError[F, Throwable]): F[A] =
    F.fromEither(AccumulatingErrorsDecoder[A].decodeJson(json).leftMap { nel =>
      new JsonDecodingError(nel)
    })
}

object JsonRequestDecoder {
  final def apply[F[_]: MonadError[*[_], Throwable]: JsonDecoder, A](implicit
      decoder: Decoder[A]
  ): JsonRequestDecoder[F, A] = new JsonRequestDecoder[F, A]
}
