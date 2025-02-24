package com.crawler.rest

import cats.Applicative
import cats.syntax.eq.*
import cats.effect.Sync
import cats.instances.option.*
import cats.syntax.applicative.*
import cats.syntax.apply.*
import com.crawler.AppConfig
import org.http4s.{BasicCredentials, Request}
import org.http4s.headers.Authorization

trait Security[F[_]] {
  def isInternalApiTokenValid: Request[F] => F[Boolean]
}

object Security {
  final class Impl[F[_]: Applicative] private (config: AppConfig.Api)
      extends Security[F] {
    def isInternalApiTokenValid: Request[F] => F[Boolean] = { req =>
      val requestToken =
        req.headers
          .get[Authorization]
          .flatMap(a => BasicCredentials.unapply(a.credentials))
          .map(_._2)

      (requestToken, config.secretKey)
        .mapN(_ === _.value)
        .getOrElse(false)
        .pure[F]
    }
  }

  object Impl {
    def make[F[_]: Sync](config: AppConfig.Api): Security[F] =
      new Impl(config)
  }
}
