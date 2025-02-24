package com.crawler.core

import cats.Parallel
import cats.effect.syntax.paralleln.*
import cats.effect.{Concurrent, Resource, Sync}
import cats.syntax.all.*
import com.crawler.common.{HTMLStreamParser, HTTPClientResource}
import com.crawler.{HTMLStreamParserError, HTTPClientError, Logger}
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Uri
import org.http4s.{MediaType, Response, Uri => Http4sUri}
import fs2.Stream

import scala.concurrent.ExecutionContext

trait Crawler[F[_]] {
  def fetchTitles(
      urls: List[String Refined Uri]
  ): F[List[Crawler.TitleFetchingResult]]

  def streamTitles(
      urls: List[String Refined Uri]
  ): Stream[F, Crawler.TitleFetchingResult]
}

object Crawler {
  sealed trait Result

  object Result {
    final case class Success(
        title: String
    ) extends Result
    final case class Failure(
        error: String
    ) extends Result
  }

  final case class TitleFetchingResult(
      url: String,
      result: Result
  )

  final class Impl[F[_]: Concurrent: Parallel](
      httpClient: HTTPClientResource[F],
      parser: HTMLStreamParser[F],
      threadsCount: Int
  )(implicit
      ecResource: Resource[F, ExecutionContext],
      logger: Logger[F]
  ) extends Crawler[F] {
    def streamTitles(
        urls: List[String Refined Uri]
    ): Stream[F, TitleFetchingResult] =
      Stream.resource(ecResource).flatMap { implicit ec =>
        Stream
          .emits(urls)
          .covary[F]
          .parEvalMap(threadsCount)(getTitle)
      }

    def fetchTitles(
        urls: List[String Refined Uri]
    ): F[List[TitleFetchingResult]] =
      ecResource.use { implicit ec: ExecutionContext =>
        urls.parTraverseN(threadsCount)(getTitle)
      }

    private def getTitle(url: String Refined Uri): F[TitleFetchingResult] = {
      fetchTitle(url).map {
        case Left(httpError) =>
          TitleFetchingResult(url.value, Result.Failure(httpError.message))
        case Right(Left(parseError)) =>
          TitleFetchingResult(url.value, Result.Failure(parseError.message))
        case Right(Right(title)) =>
          TitleFetchingResult(url.value, Result.Success(title))
      }
    }

    private def fetchTitle(
        url: String Refined Uri
    ): F[Either[HTTPClientError, Either[HTMLStreamParserError, String]]] = {
      for {
        uriEither <- Sync[F].pure(toHttp4sUri(url))
        result <- uriEither match {
          case Left(error) =>
            Sync[F].pure(
              Left(error): Either[
                HTTPClientError,
                Either[HTMLStreamParserError, String]
              ]
            )
          case Right(uri) =>
            httpClient.get(uri).use {
              case Left(error) =>
                Sync[F].pure(
                  Left(error): Either[
                    HTTPClientError,
                    Either[HTMLStreamParserError, String]
                  ]
                )
              case Right(response) =>
                validateResponse(response, url) match {
                  case Left(error) =>
                    Sync[F].pure(
                      Left(error): Either[
                        HTTPClientError,
                        Either[HTMLStreamParserError, String]
                      ]
                    )
                  case Right(validResponse) =>
                    parser
                      .title(validResponse.body)
                      .map(parsed =>
                        Right(parsed): Either[
                          HTTPClientError,
                          Either[HTMLStreamParserError, String]
                        ]
                      )
                }
            }
        }
      } yield result
    }

    private def toHttp4sUri(
        url: String Refined Uri
    ): Either[HTTPClientError, Http4sUri] = {
      Http4sUri
        .fromString(url.value)
        .leftMap(_ => HTTPClientError.InvalidUrl(url.value))
    }

    private def validateResponse(
        response: Response[F],
        url: String Refined Uri
    ): Either[HTTPClientError, Response[F]] = {
      if (response.status.isSuccess) {
        if (response.contentType.exists(_.mediaType === MediaType.text.html)) {
          Right(response)
        } else {
          Left(HTTPClientError.InvalidContentType(url.value))
        }
      } else {
        {
          Left(HTTPClientError.UnexpectedStatus(response.status.toString))
        }
      }
    }
  }

  object Impl {
    def make[F[_]: Concurrent: Parallel](
        client: HTTPClientResource[F],
        parser: HTMLStreamParser[F],
        threadsCount: Int
    )(implicit
        ecResource: Resource[F, ExecutionContext],
        logger: Logger[F]
    ): Crawler[F] = new Impl[F](client, parser, threadsCount)
  }
}
