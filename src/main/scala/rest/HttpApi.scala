package com.crawler.rest

import cats.effect.{Concurrent, Timer}
import cats.{Defer, MonadError}
import com.crawler.core.Crawler
import org.http4s.CharsetRange.*
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.middleware.Logger
import org.http4s.syntax.kleisli.*
import org.http4s.{HttpApp, HttpRoutes}

trait HttpApi[F[_]] extends Http4sDsl[F] {
  def app: HttpApp[F]
}

object HttpApi {
  final class Impl[
      F[_]: MonadError[*[_], Throwable]: Concurrent: Defer: JsonDecoder: Timer
  ] private (
      val security: Security[F],
      val algebra: Crawler[F]
  ) extends HttpApi[F] {
    def app: HttpApp[F] =
      Logger
        .httpRoutes[F](
          logHeaders = false,
          logBody = true
        )(routes)
        .orNotFound

    private def routes: HttpRoutes[F] =
      new Router[F](security, algebra).routes
  }

  object Impl {
    def make[
        F[_]: MonadError[*[_], Throwable]: Concurrent: Defer: JsonDecoder: Timer
    ](
        security: Security[F],
        algebra: Crawler[F]
    ): HttpApi[F] =
      new Impl[F](security, algebra)
  }
}
