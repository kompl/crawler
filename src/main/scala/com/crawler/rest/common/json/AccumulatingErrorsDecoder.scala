package com.crawler.rest.common.json

import cats.data.NonEmptyList
import com.crawler.JsonError
import io.circe.*

final class AccumulatingErrorsDecoder[A] {
  final def decodeJson(
      json: Json
  )(implicit decoder: Decoder[A]): Either[NonEmptyList[JsonError], A] =
    decoder
      .decodeAccumulating(HCursor.fromJson(json))
      .toEither match {
      case Right(p) => Right(p)
      case Left(failures) =>
        Left(
          failures
            .map {
              case DecodingFailure(m, history) =>
                JsonError.InvalidKey(CursorOp.opsToPath(history), m)
              case _ =>
                JsonError.ParsingFailed("Unexpected error")
            }
        )
    }
}

object AccumulatingErrorsDecoder {
  final def apply[A]: AccumulatingErrorsDecoder[A] =
    new AccumulatingErrorsDecoder[A]
}
