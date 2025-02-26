package com.crawler.rest.v1

import cats.effect.{Concurrent, Timer}
import cats.syntax.all.*
import com.crawler.core.Crawler
import com.crawler.rest.common.auth.AuthedRouter
import com.crawler.rest.common.error_handling.{ErrorHandler, HttpErrorHandler}
import com.crawler.rest.common.json.JsonRequestDecoder
import com.crawler.rest.v1.codecs.*
import fs2.text.utf8Encode
import io.circe.syntax.EncoderOps
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.AuthMiddleware
import org.http4s.{AuthedRoutes, HttpRoutes}
import fs2.Stream

final class Router[
    F[_]: Concurrent: JsonDecoder: Timer
](
    authMW: AuthMiddleware[F, Unit],
    algebra: Crawler[F]
) extends Http4sDsl[F] {

  private val errorHandler: HttpErrorHandler[F, Throwable] = ErrorHandler[F]

  private val httpRoutes: AuthedRoutes[Unit, F] =
    AuthedRoutes.of[Unit, F] {
      case r @ PUT -> Root as () =>
        (for {
          post <- JsonRequestDecoder[F, ExtractTitlesPost].decodeRequest(r.req)
          resultStream =
            algebra
              .streamTitles(post.sites)
              .map(result => result.asJson.noSpaces)
              .intersperse(",")
          jsonArrayStream = Stream.emit("[") ++ resultStream ++ Stream.emit("]")
          encoded = jsonArrayStream.through(utf8Encode[F])
        } yield Ok(encoded)).flatten

      case r @ POST -> Root as () =>
        (for {
          post <- JsonRequestDecoder[F, ExtractTitlesPost].decodeRequest(r.req)
          res <- algebra.fetchTitles(post.sites)
        } yield res).flatMap(Ok(_))
    }

  def routes: HttpRoutes[F] =
    AuthedRouter(
      authMW,
      "fetchTitles" -> errorHandler.handle(httpRoutes)
    )
}
