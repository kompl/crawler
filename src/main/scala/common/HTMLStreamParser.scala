package com.crawler.common

import cats.effect.Async
import cats.syntax.all.*
import com.crawler.HTMLStreamParserError
import fs2.Stream
import fs2.text.utf8Decode

trait HTMLStreamParser[F[_]] {
  def title(stream: Stream[F, Byte]): F[Either[HTMLStreamParserError, String]]
}

object HTMLStreamParser {
  final class Impl[F[_]: Async] extends HTMLStreamParser[F] {

    def title(
        stream: Stream[F, Byte]
    ): F[Either[HTMLStreamParserError, String]] = {
      val titleStart = "<title>"
      val titleEnd = "</title>"

      stream
        .through(utf8Decode)
        .flatMap { chunk =>
          val startIdx = chunk.indexOf(titleStart)
          if (startIdx >= 0) {
            val endIdx = chunk.indexOf(titleEnd, startIdx + titleStart.length)
            if (endIdx >= 0) {
              Stream.emit(chunk.substring(startIdx + titleStart.length, endIdx))
            } else Stream.empty
          } else Stream.empty
        }
        .compile
        .last
        .map(handleFound)
    }

    private def handleFound(
        title: Option[String]
    ): Either[HTMLStreamParserError, String] =
      title match {
        case Some(title) => title.asRight
        case None =>
          HTMLStreamParserError.TitleNotFound("Title not found").asLeft
      }
  }

  object Impl {
    def make[F[_]: Async]: HTMLStreamParser[F] = new Impl[F]
  }
}
