package com.crawler.rest

import cats.effect.{Concurrent, Timer}
import com.crawler.core.Crawler
import com.crawler.rest.common.auth.Middleware
import org.http4s.HttpRoutes
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

final class Router[
    F[_]: Concurrent: JsonDecoder: Timer
](security: Security[F], algebra: Crawler[F])
    extends Http4sDsl[F] {

  private def v1Router: HttpRoutes[F] = {
    val authMW = Middleware[F](security.isInternalApiTokenValid)

    new v1.Router[F](authMW, algebra).routes
  }

  def routes: HttpRoutes[F] =
    Router[F](
      "v1" -> v1Router
    )
}
