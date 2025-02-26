package com.crawler.rest.common.auth

import cats.Monad
import cats.data.{Kleisli, OptionT}
import cats.syntax.functor.*
import io.circe.Json
import io.circe.syntax.EncoderOps
import org.http4s.circe.jsonEncoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.AuthMiddleware
import org.http4s.{AuthedRoutes, Request}

final case class Authorizer[F[_]: Monad](
    credentialsVerifier: Request[F] => F[Boolean]
) extends Http4sDsl[F] {

  val unauthorized: AuthedRoutes[Unit, F] = Kleisli { _ =>
    OptionT.liftF(
      Forbidden(
        Json.obj(
          "code" -> "Forbidden".asJson,
          "message" -> "".asJson
        )
      )
    )
  }

  val handle: Kleisli[F, Request[F], Either[Unit, Unit]] =
    Kleisli { req =>
      credentialsVerifier(req).map {
        case true  => Right(())
        case false => Left(())
      }
    }
}

object Middleware {

  def apply[F[_]: Monad](
      credentialsVerifier: Request[F] => F[Boolean]
  ): AuthMiddleware[F, Unit] = {
    val auth = Authorizer(credentialsVerifier)

    AuthMiddleware[F, Unit, Unit](auth.handle, auth.unauthorized)
  }
}
