package com.crawler.rest.common.auth

import cats.Monad
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.http4s.server.{AuthMiddleware, Router}

object AuthedRouter {
  def apply[F[_]: Monad](
      mw: AuthMiddleware[F, Unit],
      mappings: (String, AuthedRoutes[Unit, F])*
  ): HttpRoutes[F] = {
    val routes: Seq[(String, HttpRoutes[F])] = mappings.map {
      case (path, authedRoutes) =>
        val rs: HttpRoutes[F] = mw(authedRoutes)
        path -> rs
    }

    Router[F](routes: _*)
  }
}
